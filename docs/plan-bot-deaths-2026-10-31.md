# Bot deaths after #37: analysis and fix plan (2026-10-31)

Data: game log from 13:19 to 14:12 UTC (~55 min after the #37 deploy), 293 deaths, ~400 bots online.
The code was read from the image sources (main at d2bc440).

## Stats after the #37 deploy (13:19 UTC)

| Metric | Before #37 (~30 min) | First 10 min after | 55 min after |
|---|---|---|---|
| Bots online | ~130-150 | 366 | 393-410 |
| Deaths | 122 | 25 | 293 (~57 per 10 min) |
| Orc Mystic deaths at level 1-5 | 22 | 2 | 55 (26 drowned) |
| Deaths from monsters 10+ levels above | 25 | 5 | 30 (8 of them in a party) |
| "Duplicate entry ... PRIMARY" errors | 61 in 8 min | 0 | 0 |
| Exceptions | - | 0 | 0 |
| "marked dangerous" / "logged in next to" | - | 1 / 5 | 7 / 5 (all 5 went back to town) |

Bots in the DB: 1110, by level: 1-5: 672 (188 online), 6-10: 380 (192 online), 11-15: 58 (26 online).
Deaths at level 1-10 by class: Orc Mystic 55, Dark Mystic 45, Human Mystic 37, Dark Fighter 34, Orc Fighter 33,
Human Fighter 26, Elven Fighter 20, Dwarven Fighter 16, Elven Mystic 12.
Top killers: the bot itself (drowning) 76, Elder Prairie Keltir 20, Kaboo Orc Fighter 18, Tomb Guard 14, Imp 12,
Orc Grunt 11, Crypt Preacher 11, Orc 10, Goblin Raider 9, Garum Werewolf 9.
The Mac slept once, 14:07:32-14:09:34 UTC, and the server kept going afterwards.

Raw logs: `logs/deaths-2026-10-31.txt`, `logs/sessions-2026-10-31.txt` (log before #37).

## What the log shows

| Cause | Deaths | Share |
|---|---|---|
| Drowning on long walks (killed by "player <self>") | 78 | 27% |
| Monsters at the level 1-10 spots (mostly Keltir, Kaboo Orc, Imp, Orc) | ~185 | 63% |
| Level 72-75 dungeon under the Dark Elf area (Tomb Guard, Crypt Preacher, Lesser Ancient Scout) | 29 | 10% |

Correction to earlier reports: `weapon NONE` in the death line is the weapon's grade (no-grade), not a missing
weapon. Bots do have weapons.

## Root causes

### 1. Drowning: newbies walk across the continent to a distant spot
- All 78 self-kills happened 20 000+ units from the bot's assigned spot, while 193 of the 222 other deaths happened
  within 5 000. Example: orc ILexI (lv 1) had a spot in the Elven area (~22 500, 7 500) and drowned four times
  in a row along the way from Orc lands (-38 800,-90 200 → -27 000,-76 500, z ≈ -4 600).
- `WaterTaskManager` deals drowning damage with the player itself as the attacker, hence "killed by player <self>".
- Why the spots are far away: `LifeBehavior` adds every empty or unreachable spot to `badSpots`, and the list is
  never cleared. With ~190 bots at level 1-5, the nearby spots are crowded and empty, so they get excluded one by one,
  and `HuntingSpots.choose` falls back to the top 5 of whatever is left, even 100k units away. A newbie has no
  adena for the gatekeeper (and is usually not in town), so it walks, and the straight path goes through water.

### 2. Newbies without potions
- 138 of 142 bots that died at level 1-5 had `potions 0`.
- The log has 180 lines of "can't afford Lesser Healing Potion at 103". `buySupplies` buys a Scroll of Escape
  first, with all of the adena (`essential`), so there is nothing left for potions. A level 1-5 bot has a few
  hundred adena at most.
- Orc Mystics also fight with their fists until they learn Life Drain at the trainer (level 7 + SP). #37 added
  Soul Cry, but without potions one Keltir is still about a 50/50 fight.

### 3. The level 72-75 dungeon: the danger marking covers one cell at a time
- The bots are assigned spots on the upper floor (cells at z -4000..-3000, monsters around level 10), and they die
  at z -4896 on the floor below, in the same xy cell area (within 5 000 of the spot).
- `reportKiller` marks only the dead bot's own spot (one 2500x2500 cell). The area has ~15 such cells, so each
  newly assigned bot picks a neighbouring unmarked cell. 7 cells were marked in 55 min, and 29 deaths still happened.
- The marks are kept only in memory and are lost on every restart.
- When the spots are built, the danger of a cell takes into account only aggressive monsters on the same floor
  (FLOOR_HEIGHT = 1000), not the floor right below.
- How exactly the bots get down to the lower floor isn't confirmed yet (a geodata hole, a path to a target, a fall).

### 4. Smaller things seen in the log
- Bone Club: demand 159, supply 0, the price grows 136 000 → 204 000 and nobody makes or sells it.
- Bots in one party refuse to lend each other items ("Stranger"): being in a party doesn't raise trust.
- No raids, crafting, enchanting or LLM requests in that hour: there are 0 crafters and the bots haven't been near a player.

## Plan, step by step

Each step is a separate PR into `l2autobots-newbies.patch` (or a new patch), deployed and checked on the Mac
for 30-60 min with the same log metrics.

1. **Stop sending newbies to distant spots** (biggest win, -27% of deaths).
   - Forget `badSpots` after N minutes, and also when the bot changes level.
   - Limit the walk distance in `HuntingSpots.choose`: without a gatekeeper (not in town or no adena), take
     only spots within ~15 000; if there are none, hunt at the best nearby spot even if it is crowded, or go to
     town and buy a teleport.
   - Check: zero "killed by player <self>" deaths, the share of deaths at 20k+ from the spot drops to ~0.

2. **Potions for newbies.**
   - At levels 1-19, buy potions before the Scroll of Escape (or buy 1 scroll and 5 potions instead of 2 scrolls
     and nothing). The scroll is useless for a newbie who hunts next to the village.
   - Optional: a starter kit when the bot is created (e.g. 10 Lesser Healing Potions, as the Newbie Guide gave),
     or let the Newbie Guide hand out potions to bots.
   - Check: `potions 0` in under half of level 1-5 deaths, "can't afford Lesser Healing Potion" lines almost gone,
     Orc Mystic deaths at level 1-5 drop at least 2x.

3. **Close off the level 72-75 dungeon for low levels.**
   - In `reportKiller`, mark all spots within ~5 000 of the death point, on every floor, not just the dead bot's own.
   - In `buildSpots`, count the danger of a cell from aggressive monsters in neighbouring cells and one floor below
     (within ~1 500 by z).
   - Save the marks in the DB (or a file) so they survive restarts.
   - Check: zero deaths from monsters 10+ levels above the bot.

4. **Find out how bots fall to the lower floor** (diagnostics before a deeper fix).
   - Log z changes > 800 for a bot at its spot (where it was, where it ended up, what it was doing).
   - Depending on the result: forbid targets on another floor, fix the spot anchor, or find the geodata hole.

5. **Orc Mystic at level 1-6** (after steps 1-2, if deaths are still high).
   - Prefer Young Keltir / level -1..0 targets until Life Drain is learned, rest at a higher HP threshold.
   - Send the bot to the trainer for Life Drain as soon as it reaches level 7 and has the SP.

6. **Smaller things** (lower priority).
   - Bone Club: find who has the demand and why nobody sells it (no NPC shop or no recipe); cap the price growth.
   - Trust: party members in a long-standing party go from "Stranger" to "Acquaintance".

## Metrics to watch after each step
Deaths per 10 min per 100 online, "killed by player <self>", deaths at level 1-5 with `potions 0`,
deaths from monsters 10+ levels above, number of "marked dangerous", errors.
