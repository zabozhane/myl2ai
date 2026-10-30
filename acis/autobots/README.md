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
- `l2autobots-fixes.patch` - upstream fixes: Kotlin 1.9 compatibility (exhaustive `when`, lambda labels,
  `minBy` -> `minByOrNull`: since Kotlin 1.7 `minBy` throws on an empty list, which killed every bot tick
  with no target around), think-loop errors logged with stack traces, and
  `sql/autobots.sql`, whose table options were a separate statement after `);` (MariaDB syntax error).
  Targeting: monsters only within bot level -5..+3 (unless they attack the bot) and in line of sight
  (geodata); `config.xml` defaults to the closest target in 1000 range instead of a random one in 2000.
- `build.gradle.kts` - compiles only the Kotlin module against aCis Java sources and copies the jar plus its
  runtime deps into `aCis_gameserver/lib`, so the stock ant build compiles and ships them.

Runtime: `data/autobots/*.xml` go to `gameserver/data/autobots`, `sql/*.sql` to `/opt/acis/sql/addons`
(applied by `init-db` on every start, the tables are `CREATE TABLE IF NOT EXISTS`).

Review of upstream code: no process execution or hidden network calls; the only outbound call is
`CrestService`, which downloads a clan crest image from a URL a GM enters in the dashboard.

## Usage
GM character (access level 7+), type `//a b` in game: the dashboard creates, spawns and controls bots.
