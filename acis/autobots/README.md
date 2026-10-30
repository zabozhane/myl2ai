# L2Autobots on aCis 382

Source: https://github.com/Elfocrash/L2Autobots, commit `f990c61d` (Kotlin, written for aCis 382), pinned in `.env`.
Everything is applied at image build time in `acis/Dockerfile`; the upstream trees are never committed here.

## Files
- `acis-382-autobots.patch` - upstream `patch.diff` rebased onto stock aCis 382 (commit `452cd109`), LF line
  endings (the Dockerfile normalizes the CRLF files it touches first). The upstream diff was taken against an
  already partly patched tree, so it lacked changes the Kotlin code depends on. Added by hand:
  - `Player`, `GameClient` not final, `Player` constructor protected: `Autobot` and `AutobotClient` extend them.
  - `Player.checkDoCastConditions` public, `Clan.getClanMembers()`: called from bot behaviors and the DAO.
  - `AdminAutobots` registered in `AdminCommandHandler`, `admin_a` (access level 7) in `adminCommands.xml`.
  - `AutobotsManager.start()` called from `GameServer`: it starts the bots' think loop.
  - Redone hunks: IdFactory (items cleanup keeps autobot items), Player (import), Say2 (`onChat` hook).
  - `RequestBypassToServer`: null check moved before `player.isGM()` (upstream could NPE).
  - Not from upstream: `AutoLootAdena` in `server.properties` (on): adena goes straight to the killer's
    inventory for everyone, other drops stay on the ground.
- `l2autobots-fixes.patch` - upstream fixes: Kotlin 1.9 compatibility (exhaustive `when`, lambda labels,
  `minBy` -> `minByOrNull`: since Kotlin 1.7 `minBy` throws on an empty list, which killed every bot tick
  with no target around), think-loop errors logged with stack traces, and
  `sql/autobots.sql`, whose table options were a separate statement after `);` (MariaDB syntax error).
  Targeting: monsters only within bot level -5..+3 (unless they attack the bot) and in line of sight
  (geodata); `config.xml` defaults to the closest target in 1000 range instead of a random one in 2000.
  Looting (`LootPicker`): bots pick up their own and ownerless drops within 400, one per tick, before fighting
  back; below 40% HP they fight instead. Items over the weight or slot limit are skipped; a pick-up the AI
  ignores (mid-attack or mid-cast) is retried up to 3 times.
  Living population (`population/`, on by default, `populationSize` in `config.xml`, 0 disables it):
  - `PopulationManager` creates level 1 bots like the client does (starter class, race village, starter kit)
    and logs them in and out every 15 s. Bots are marked `Population` in `activity_prefs`; bots made from
    the dashboard are left alone.
  - `DailySchedule`: per bot chronotype (wake time around 08:00 +- 2.5 h, Moscow time), 8 h sleep and 2-4
    sessions of 1-3 h a day, more likely in the evening. `Career`: play periods of 2-60 days, breaks of
    2-30 days, 30% chance to quit after each period. Both are derived from the object id, nothing is stored.
  - Newbies replace inactive players, up to `populationNewbiesPerDay` spread over the day; a fresh server
    opens with `populationSize` of them at once.
  - `LifeBehavior`: a session is hunting (90%) or AFK. Hunting spots come from monster spawns (2500 cells
    with 4+ monsters of level -4..+2), weighted towards close and less crowded ones. Far spots are reached by
    a paid gatekeeper teleport, near ones on foot; empty, crowded (6+ players) or unreachable spots are
    dropped and another one is picked.
  - Self-defense: a population bot fights back against a flagged or PK player who attacks it (GMs included),
    but never starts PvP itself.
  - Nothing from thin air any more: no gear on spawn or level up, no free shots, potions, arrows, buffs or
    mana; bots use what they own and sit down to regenerate (`Rester`).
- `l2autobots-autofarm.patch` - auto hunt for live players (Essence style), applied after the fixes patch.
  Built on the upstream `autofarm/` stub, which ran the bots' `CombatBehavior` for a `Player` but could not be
  opened without the GM dashboard. Spec: `docs/autofarm.md`.
  - `.autofarm` in chat toggles it, `.autofarm on|off|menu`; the line never reaches the chat. On first login
    every character gets the `AutoHunt` and `AutoHuntSet` macros on the rightmost free slots of the shortcut
    bar (a memo flag, so a deleted macro is not brought back).
  - Hunts within the radius (600/1000/1500) around the spot where it was started, walks back when a fight pulls
    it out; raid bosses off by default. Never attacks players (warns when one attacks), never revives: it stops
    on death, in town/peace/siege zones, the Olympiad, store mode, on class change, teleport away and logout.
    Any click, move, skill or attack by hand pauses it for 3 s.
  - Potions only from the inventory with HP/MP/CP thresholds, sitting to regenerate and loot pick-up can be
    turned off, any class skill can be disabled. Settings window in the tutorial window, stored per character
    in `character_autofarm_settings` (JSON); class preferences stay in upstream `character_autofarm`.
  - Upstream bugs fixed on the way: the busy check used `ConcurrentHashMap.contains` (a value lookup), so ticks
    overlapped; preferences of every class were read with the active class types; `onEnterWorld` could throw
    for a class without a behavior.
- `build.gradle.kts` - compiles only the Kotlin module against aCis Java sources and copies the jar plus its
  runtime deps into `aCis_gameserver/lib`, so the stock ant build compiles and ships them.

Runtime: `data/autobots/*.xml` go to `gameserver/data/autobots`, `sql/*.sql` to `/opt/acis/sql/addons`
(applied by `init-db` on every start, the tables are `CREATE TABLE IF NOT EXISTS`).

Review of upstream code: no process execution or hidden network calls; the only outbound call is
`CrestService`, which downloads a clan crest image from a URL a GM enters in the dashboard.

## Usage
GM character (access level 7+), type `//a b` in game: the dashboard creates, spawns and controls bots.
Any player: the `AutoHunt` macro on the shortcut bar (or `.autofarm` in chat) toggles auto hunt, `AutoHuntSet`
(`.autofarm menu`) opens its settings.
