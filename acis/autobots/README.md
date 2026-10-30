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
  - Not from upstream: `RequestJoinParty` calls `AutobotsManager.onPartyInvite`: a bot never sees the
    AskJoinParty window, so it answers the invite in code.
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
  - `LifeBehavior`: a session is a chain of segments (see `l2autobots-sessions.patch`). Hunting spots come from monster spawns (2500 cells
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
  Professions (`behaviors/classes/`): every Interlude class plays now. Healers (Cleric line, Elven and Shillien
  Oracle line) heal by priority (tank first, group heal when 2+ members are hurt, battle heals below 35%), raise
  the dead, recharge and buff; buffers (Prophet, Warcryer line) keep party buffs and chants up; summoners
  (Warlock, Elemental and Phantom Summoner lines) call, heal, recharge and buff their servitor. Summons consume
  the crystals the skill asks for: dwarves with Crystallize turn junk gear into crystals in town and sell them in
  stores, summoners buy them there. The behavior is rebuilt after a class change. `professionWeight*` in
  `config.xml` tune how often bots pick a healer, buffer, tank or summoner branch; the summary shows `roles=`.
  Parties (`population/group/`, `groupsEnabled`):
  - `PartyBehavior`: members follow the leader (gatekeeper or SoE when far), assist the agreed target, the tank
    or the leader, fight back, sit when the leader sits; the leader rests when a healer is low on mana or a member
    is hurt, and picks spots for the party's level (+1..+3 with a healer and more members).
  - A dead member accepts a raise (the ConfirmDlg answered in code) and waits up to 2 minutes for one when a
    resurrector is near.
  - `PartyInvites`: a bot answers `/invite` after 2-6 s by its sociability (loner 30%, regular 70%, team
    player 95%; a bot that needs a group or a healer almost always accepts), refusing PKs, busy states and a
    level gap of 8+, and whispers why now and then.
  - `GroupChat`: party chat is a teammate's suggestion, never an order. "assist me", "rest", "buff",
    "let's move", "brb", "bye" (Russian or English keywords) are weighed against the party's state and answered
    in the player's language.
  - `GroupFinder` (`groupFinderEnabled`, every 30 s): team players in 60% of sessions from `groupFinderMinLevel` (10), regulars in
    25%, and anyone who keeps dying look for a group. Lookers of one map region within 5 levels team up behind
    the most sociable one: a healer from level 25 (or damage dealers after 5 minutes without one), then tank,
    buffer, spoiler and damage dealers, 3-5 members, people they dislike left out. At most
    `groupMaxPopulationPercent` of the bots online play in parties. A bot-led party with free places shouts for
    the missing roles every 3-5 minutes and breaks up after 10 minutes below 3 members. `groupLog` prints formed
    parties.
  - Players: "ищу пати" / "lfp" in public chat brings whispered offers from up to two fitting bot-led parties
    (who, levels, where). A whisper to a leader with room ("+", "invite", "возьмите") makes it inspect the
    player like a player would (level, karma, a role the party lacks, weapon grade for the level; a friend is
    taken with weaker gear) and invite or explain why not. First come, first served: the place is held for a
    minute while the invite is open.
  - `Relations` (`autobot_relations`): each bot remembers up to 50 characters it played with: minutes together
    and a liking that grows with play and help and drops with wipes. Groups prefer friends. Clans (4a) and
    pooling (4b) build on it; trust is for social trading (2g).
  - `populationTestLevel` (test stands only, 0 by default): new population bots start at that level with gear of
    the level, so groups can be watched without days of play.
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
- `l2autobots-craft.patch` - crafting for population bots (stage 2v), applied after the autofarm patch so the groups
  and economy work keeps its own diffs. New code in `population/economy/craft/`, `craftEnabled` and
  `crafterArtisanPercent` in `config.xml`. Everything goes through the core: the `Recipes` item handler,
  `RecipeItemMaker` (own craft and Dwarven Manufacture).
  - Crafters are the Artisan line: `crafterArtisanPercent` (60) of population dwarves take Artisan at level 20 (a trait
    of the object id), the others Scavenger. Any dwarf with Create Item crafts what its book allows.
  - `Crafter` (in town, during `TownTrip`): learns the recipes it carries (book limit and skill level checked by the
    handler; recipes up to 5 levels ahead are kept), buys missing materials in stores and shops (town NPCs sell only Soul and Spirit Ore, Gemstones and Rope of Magic),
    then crafts: intermediates first, then the best recipe by demand and margin, waiting for MP sitting. Unwanted gear
    stops at one piece; shots and materials go on while materials last. `Crystals` (dwarves break surplus gear into
    crystals for summoners) skips what the crafter made itself.
  - `CraftPlanner`: demand = next kit pieces and shots of the bots online (refreshed each minute); craft cost = own
    materials at the NPC sell price + missing ones at the best town price, divided by the success rate.
  - `Workshop`: a crafter's store session is a Dwarven Manufacture 40% of the time (up to 20 recipes, fee 5-15% of the
    product value), otherwise a sell store of its work (never below its cost + 10%) or a buy store for its shopping list
    and for recipes in demand it could learn. Crafters trade in 30% of their sessions (others 10%) and linger in 60%.
    Store ads go to trade chat (`+`) at most every 15-30 minutes.
  - `Sourcing`: for its next kit pieces a bot compares shop, private store, workshop order (fee + missing materials
    + its own materials at NPC value, / success rate) and its own craft, and orders when the workshop is cheapest; a
    store price above the order cost is no longer accepted. Gear above the bot's grade is never ordered.
  - `CraftStock`: materials, parts and recipes for the bot's own crafts and next kit pieces are kept, up to 25% of the
    weight limit (no warehouse yet). Materials and recipes are never sold to NPCs as junk: they go to stores (stacks
    worth 300+) or to crafters' buy stores; a bag over 40% weight still sells them.
  - `Prospecting`: half of the Scavenger line (a trait of the object id) picks hunting spots whose monsters give the
    materials crafters want when spoiled; a fifth of other hunters pick spots that drop the parts of their next kit
    piece. Spot scores get up to 5x for matching monsters (`HuntingSpots.lootBonus`).
  - Recipe books live in `autobots_recipebook` (`sql/autobots_recipebook.sql`): the core purges `character_recipebook`
    rows of ids missing from `characters` at every start.
  - The Ivory Tower sales cat (31075, a GM shop with crystals, recipes and materials) is excluded from the shop index
    even if someone spawns it.
  - The summary line gets `Craft CraftFail CraftOrder RecipeLearn Workshop` counts and
    `crafters workshops recipes adenaPerCrafter adenaPerOtherSameLevel`.
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
- `l2autobots-sessions.patch` - a play session made of segments (`population/SessionPlan.kt`), applied last; it
  replaces the one-activity-per-session choice (90% hunting, otherwise AFK or a store for the whole session).
  - Segments: hunting (quests and town trips included), trading in a store, AFK, and going home. Each bot has a
    stable style from its object id: grinder 30% (hunts 60-120 min, rare breaks, rarely trades), casual 50%
    (30-75 min, more breaks), trader 20% (20-50 min of hunting, stores of 30-90 min); crafters are always traders.
  - Segments end on time or for a player's reasons: after a death (a break or a store), a new level (a short break),
    a town trip that leaves goods to sell (a trader stays behind a store). The longer a bot hunts without a break,
    the likelier the next segment is AFK; at night (01-06 Moscow) breaks are twice as likely and 1.5x longer.
  - Never AFK the whole session: AFK lasts 3-25 min, never follows AFK, is 2-8 min right after login, and a bot with
    a pending profession quest (`QuestRunner.hasBusiness`) only takes 2-8 min breaks after hunting and opens no stores.
  - A fight is finished before switching. AFK outside town steps away from aggressive monsters first (up to 3 times).
    10-20 min before the scheduled end (`DailySchedule.minutesLeft`) a bot walks or escapes to town, visits the shops
    and waits for the logout there (or keeps a store, as before). AFK and going-home bots do not join parties.
  - `populationSessionLog` prints every switch with its reason (`Autobots session: <name> lv N Hunt -> Afk for 12 min
    (died)`); `Autobots session summary:` every 10 minutes counts hunting, trading, AFK, going home and party bots.
