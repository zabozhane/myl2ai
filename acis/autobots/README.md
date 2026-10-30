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
  - `populationAlwaysOnline` (load testing): every active member stays online regardless of the schedule.
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
  Living economy (`population/economy/`, `economyEnabled` in `config.xml`): population bots live on what they
  earn; nothing is created, every deal goes through the same code paths a client request would.
  - `Market`: town shops indexed from merchant spawns and buy lists (clan hall stewards excluded). Buying and
    selling mirror `RequestBuyItem`/`RequestSellItem` (castle tax, stock, half the reference price on sale);
    private stores are used through `TradeList.privateStoreBuy/Sell`, so players and bots trade both ways.
    Town shops sell No-grade and D-grade weapons and armor only; higher grades and enchant scrolls come from drops
    and private stores.
  - `TownTrip`: a bot leaves its spot for town (Scroll of Escape, or on foot if close) when its bag passes 45% of
    the weight limit (the penalty starts at 50%), it runs out of shots or arrows, it can afford the next kit piece,
    or every 90 minutes with loot to sell; a death in town also triggers a visit. In town: private stores first
    (buy below shop price, sell above NPC price), then merchants (junk out; Scroll of Escape, grade-matched shots,
    arrows, potions, the next kit piece in), then equip and enchant. The target kit is `equipment.xml`, which
    used to be handed out for free.
  - `Enchanter`: worn gear with the bot's own scrolls, chances from `EnchantScroll.getChance`; a failed normal
    scroll crystallizes the item, a blessed one resets it. The temper (from the object id) sets the risk:
    50% cautious (safe levels only), 35% normal (armor +4, weapon +6 if the adena would replace the item), 15%
    gamblers (armor +6, weapon +10).
  - `StoreKeeper`: AFK sessions of bots with goods become store sessions, and 30% of bots with goods stay online
    30-90 minutes after their session in a store. Valuable surplus (materials, recipes, gear, scrolls: stacks worth
    2000+) goes to a sell store at 0.65-0.9 of the shop price, or 0.8-1.5 of the reference price for items no shop
    sells; unsold items get 10% cheaper per session. With no goods, a buy store bids on the next kit piece shops
    don't sell and on scrolls for worn gear, paid with the bot's own adena.
  - `Fitness`: 3 deaths in an hour or gear more than a grade below the level switch the bot to farming monsters
    5 levels lower, saving for gear and enchanting safely; dying again there sets `needsGroup` (for the group
    stage) and drops 3 more levels. An hour without deaths with decent gear brings it back.
  - Population bots have a real weight limit and inventory size again; shots and arrows match the weapon grade;
    bots use shop Healing Potions and Mana Drugs. `economyLog` logs every deal, `economyChat` lets bots remark on
    enchants and upgrades in chat.
  - Every 10 minutes one `Autobots economy summary:` line counts the deals by kind and shows the bots online
    (average and max level, adena, stores, farming, needsGroup). `scripts/economy-report.sh [since]` prints those
    summaries, deal counts with samples, bot errors and the game container's load.
- `build.gradle.kts` - compiles only the Kotlin module against aCis Java sources and copies the jar plus its
  runtime deps into `aCis_gameserver/lib`, so the stock ant build compiles and ships them.

Runtime: `data/autobots/*.xml` go to `gameserver/data/autobots`, `sql/*.sql` to `/opt/acis/sql/addons`
(applied by `init-db` on every start, the tables are `CREATE TABLE IF NOT EXISTS`).

Review of upstream code: no process execution or hidden network calls; the only outbound call is
`CrestService`, which downloads a clan crest image from a URL a GM enters in the dashboard.

## Usage
GM character (access level 7+), type `//a b` in game: the dashboard creates, spawns and controls bots.
