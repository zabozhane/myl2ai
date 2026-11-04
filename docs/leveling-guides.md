# Гайд прокачки 1–80 для нашего сервера и расхождения с ботами

Статус: черновик-справочник и план. Поведение ботов этим документом **не меняется**: изменения из раздела «План»
делаются только после одобрения владельца.

## Коротко

- Сервер — **Lineage 2 Interlude (C6, протокол 746)** на **aCis 382** (коммит `452cd109`, `.env: ACIS_COMMIT`),
  боты — L2Autobots `f990c61d` с нашими патчами. Рейты x1 (`RateXp = RateSp = 1`), `AutoLearnSkills=False`,
  `SpBookNeeded=True`. То есть это ретейл-подобный Interlude, и классические гайды Interlude к нему применимы.
- Нормальных подробных гайдов «1–80 по каждому классу» под Interlude в открытом доступе почти не осталось. То, что
  находится поиском (например, l2db.net), — сгенерированные тексты с выдуманными зонами и квестами («Kamael»,
  «Mark of a Varkas»), их использовать нельзя. Поэтому гайд ниже собран из общепринятой прогрессии Interlude и
  **проверен по данным самого сервера**: уровни мобов в каждой зоне посчитаны по `spawnlist.sql` и `npcs/*.xml`,
  марки профессий — по `SecondClassChange.java`, книги — по `spellbooks.xml` и `buyLists.xml`. Где данные сервера и
  память расходятся, верны данные.
- Главные расхождения с ботами: население **застревает на 1-й профессии** (2-я и 3-я не скриптованы, бесплатной
  смены нет), жители 40+ и монки перестают покупать экипировку (нет строк набора), у магов D-набор фактически
  No-grade, в наборах S есть невозможные комбинации (дуалы/двуручник + щит, маг в A-робе со щитом), выбор зоны
  кача не знает про класс и держит бота у дома, нет повторяемых квестов на 42+.

## 1. Версия и что из неё следует

| Параметр | Значение | Где |
|---|---|---|
| Хроника | Interlude (C6), клиент 746 | `README.md` |
| Ядро | aCis 382, `ACIS_COMMIT=452cd109c3bce48416f74bba4daa8c3cf5dfc0a0` | `.env`, `acis/Dockerfile` |
| Боты | L2Autobots `f990c61d` + патчи `acis/autobots/*.patch` | `.env`, `acis/Dockerfile` |
| Рейты | XP/SP/адена/дроп/квесты x1 | `aCis_gameserver/config/server.properties` |
| Скиллы | учатся у тренера за SP, книги обязательны | `docs/quests.md` |
| Геодата | экспорт эпохи Gracia/HF (местами не родная IL) | `README.md` |

Следствия для гайда: шкала грейдов Interlude (NG 1–19, D 20–39, C 40–51, B 52–60, A 61–75, S 76+), 3 профессии
(20/40/76), опыт только с мобов и квестов, без ускорителей. Камаэлей, Hellbound и прочего из более поздних хроник
нет.

## 2. Профессии

### 2.1. Первая профессия (20 ур., квест с 19)

Квест «Path to …» своей расы и ветки → знак → мастер гильдии (`FirstClassChange`) меняет класс и выдаёт 15 купонов
на теневое оружие D. Все 18 квестов уже сыграны ботами (`population/quests/PathQuests.kt`).

| Квест | Мин. ур. | Стартовый NPC (id) и точка |
|---|---|---|
| Q401_PathToAWarrior | 19 | Auron (30010) (-81861, 149197) |
| Q402_PathToAHumanKnight | 19 | Sir Klaus Vasper (30417) (-82437, 150282) |
| Q403_PathToARogue | 19 | Bezique (30379) (-85019, 152887) |
| Q404_PathToAHumanWizard | 19 | Parina (30391) (-80430, 150694) |
| Q405_PathToACleric | 19 | Zigaunt (30022) (-79256, 150676) |
| Q406_PathToAnElvenKnight | 19 | Sorius (30327) (-13440, 122643) |
| Q407_PathToAnElvenScout | 19 | Reisa (30328) (-13693, 122583) |
| Q408_PathToAnElvenWizard | 19 | Rosella (30414) (43673, 49683) |
| Q409_PathToAnElvenOracle | 19 | Manuel (30293) (-13487, 121541) |
| Q410_PathToAPalusKnight | 19 | Virgil (30329) (-12791, 123259) |
| Q411_PathToAnAssassin | 19 | Triskel (30416) (-12506, 123405) |
| Q412_PathToADarkWizard | 19 | Varika (30421) (-44225, 79721) |
| Q413_PathToAShillienOracle | 19 | Sidra (30330) (-12503, 123054) |
| Q414_PathToAnOrcRaider | 19 | Karukia (30570) (-45952, -114496) |
| Q415_PathToAMonk | 19 | Gantaki Zu Urutu (30587) (-44624, -111873) |
| Q416_PathToAnOrcShaman | 19 | Tataru Zu Hestui (30585) (-45328, -114736) |
| Q417_PathToBecomeAScavenger | 19 | Pippi (30524) (115618, -183265) |
| Q418_PathToAnArtisan | 19 | Silvera (30527) (115271, -182692) |

(Точки: Глудин около -82000,150000; Глудио около -13000,123000; эльфийская деревня 43673,49683; орочья деревня
около -45000,-114000; гномья деревня около 115000,-183000. Q412 Варика — у Школы тёмных искусств.)

### 2.2. Вторая профессия (40 ур.): три марки

Мастер гильдии (`scripts/village_master/SecondClassChange.java`) меняет класс только при трёх марках. Наборы марок
взяты из скрипта сервера:

| 1-я профессия | 2-я профессия | Марки: квест, марка (id предмета) |
|---|---|---|
| Palus Knight | Shillien Knight | Q212 Duty (2633), Q219 Fate (3172), Q229 Witchcraft (3307) |
| Palus Knight | Bladedancer | Q211 Challenger (2627), Q219 Fate (3172), Q222 Duelist (2762) |
| Shillien Oracle | Shillien Elder | Q215 Pilgrim (2721), Q219 Fate (3172), Q227 Reformer (2821) |
| Assassin | Abyss Walker | Q213 Seeker (2673), Q219 Fate (3172), Q225 Searcher (2809) |
| Assassin | Phantom Ranger | Q213 Seeker (2673), Q219 Fate (3172), Q224 Sagittarius (3293) |
| Dark Wizard | Spellhowler | Q214 Scholar (2674), Q219 Fate (3172), Q228 Magus (2840) |
| Dark Wizard | Phantom Summoner | Q214 Scholar (2674), Q219 Fate (3172), Q230 Summoner (3336) |
| Monk | Tyrant | Q211 Challenger (2627), Q220 Glory (3203), Q222 Duelist (2762) |
| Orc Raider | Destroyer | Q211 Challenger (2627), Q220 Glory (3203), Q223 Champion (3276) |
| Orc Shaman | Overlord | Q215 Pilgrim (2721), Q220 Glory (3203), Q232 Lord (3390) |
| Orc Shaman | Warcryer | Q215 Pilgrim (2721), Q220 Glory (3203), Q233 Warspirit (2879) |
| Scavenger | Bounty Hunter | Q216 Guildsman (3119), Q221 Prosperity (3238), Q225 Searcher (2809) |
| Artisan | Warsmith | Q216 Guildsman (3119), Q221 Prosperity (3238), Q231 Maestro (2867) |
| Elven Knight | Temple Knight | Q212 Duty (2633), Q218 Life (3140), Q226 Healer (2820) |
| Elven Knight | Sword Singer | Q211 Challenger (2627), Q218 Life (3140), Q222 Duelist (2762) |
| Human Knight | Paladin | Q212 Duty (2633), Q217 Trust (2734), Q226 Healer (2820) |
| Human Knight | Dark Avenger | Q212 Duty (2633), Q217 Trust (2734), Q229 Witchcraft (3307) |
| Rogue | Treasure Hunter | Q213 Seeker (2673), Q217 Trust (2734), Q225 Searcher (2809) |
| Rogue | Hawkeye | Q213 Seeker (2673), Q217 Trust (2734), Q224 Sagittarius (3293) |
| Elven Scout | Plains Walker | Q213 Seeker (2673), Q218 Life (3140), Q225 Searcher (2809) |
| Elven Scout | Silver Ranger | Q213 Seeker (2673), Q218 Life (3140), Q224 Sagittarius (3293) |
| Warrior | Gladiator | Q211 Challenger (2627), Q217 Trust (2734), Q222 Duelist (2762) |
| Warrior | Warlord | Q211 Challenger (2627), Q217 Trust (2734), Q223 Champion (3276) |
| Elven Wizard | Spellsinger | Q214 Scholar (2674), Q218 Life (3140), Q228 Magus (2840) |
| Elven Wizard | Elemental Summoner | Q214 Scholar (2674), Q218 Life (3140), Q230 Summoner (3336) |
| Human Wizard | Sorcerer | Q214 Scholar (2674), Q217 Trust (2734), Q228 Magus (2840) |
| Human Wizard | Necromancer | Q214 Scholar (2674), Q217 Trust (2734), Q229 Witchcraft (3307) |
| Human Wizard | Warlock | Q214 Scholar (2674), Q217 Trust (2734), Q230 Summoner (3336) |
| Cleric | Bishop | Q215 Pilgrim (2721), Q217 Trust (2734), Q226 Healer (2820) |
| Cleric | Prophet | Q215 Pilgrim (2721), Q217 Trust (2734), Q227 Reformer (2821) |
| Elven Oracle | Elven Elder | Q215 Pilgrim (2721), Q218 Life (3140), Q226 Healer (2820) |

Где брать марочные квесты (стартовый NPC, минимальный уровень — из скриптов):

| Квест | Мин. ур. | Стартовый NPC (id) и точка |
|---|---|---|
| Q211_TrialOfTheChallenger | 35 | Kash (30644) | (19831, 144514) |
| Q212_TrialOfDuty | 35 | Hannavalt (30109) | (85655, 146471) |
| Q213_TrialOfTheSeeker | 36 | Dufner (30106) | (85234, 146634) |
| Q214_TrialOfTheScholar | 36 | Mirien (30461) | (16427, 142131) |
| Q215_TrialOfThePilgrim | 35 | Santiago (30648) | (-85002, 105749) |
| Q216_TrialOfTheGuildsman | 35 | Valkon (30103) | (83264, 146602) |
| Q217_TestimonyOfTrust | 38 | Hollint (30191) | (83634, 52927) |
| Q218_TestimonyOfLife | 38 | Cardien (30460) | (19472, 144334) |
| Q219_TestimonyOfFate | 38 | Kaira (30476) | (78928, 149041) |
| Q220_TestimonyOfGlory | 37 | Vokian (30514) | (80456, 144376) |
| Q221_TestimonyOfProsperity | 38 | Parman (30104) | (83263, 146736) |
| Q222_TestOfTheDuelist | 39 | Kaien (30623) | (79610, 56717) |
| Q223_TestOfTheChampion | 39 | Ascalon (30624) | (79253, 147049) |
| Q224_TestOfSagittarius | 39 | Bernard (30702) | (117598, 77264) |
| Q225_TestOfTheSearcher | 39 | Luther (30690) | (119472, 77835) |
| Q226_TestOfTheHealer | 39 | Bandellos (30473) | (85829, 148365) |
| Q227_TestOfTheReformer | 39 | Pupina (30118) | (86485, 148384) |
| Q228_TestOfMagus | 39 | Rukal (30629) | (16680, 142368) |
| Q229_TestOfWitchcraft | 39 | Orim (30630) | (69976, 109692) |
| Q230_TestOfTheSummoner | 39 | Galatea (30634) | (-81199, 151904) |
| Q231_TestOfTheMaestro | 39 | Lockirin (30531) | (116226, -178529) |
| Q232_TestOfTheLord | 39 | Kakai (30565) | (-46768, -113610) |
| Q233_TestOfTheWarSpirit | 39 | Somak (30510) | (19924, 143196) |

(Около 83000,147000 — Гиран; 16000–20000,142000–145000 — Дион; 79000–84000,52000–57000 — Орен; 117000–119000,77000 —
Деревня охотников; -81000,151000 — Глудин; -46000,-113000 — орочья деревня; 116000,-178000 — гномья деревня.)

Обычный порядок у игроков: «Trial» (35–36) → «Testimony» (37–38) → «Test» (39), параллельно с качем 35–40. Это самые
длинные квесты игры: путешествия по всему континенту и мобы, которых квест спавнит на игрока. Q219 Testimony of
Fate нужен всем тёмным эльфам, Q217 Trust — людям и магам-эльфам с людьми, Q220 Glory — оркам.

### 2.3. Третья профессия (76 ур.): сага

Саги Q070–Q100 (`scripting/quests/SagasScripts/`, общий `SagasSuperClass`, проверка `level >= 76`), по одной на
каждую 3-ю профессию: Q070 Phoenix Knight … Q100 Maestro. Цепочка NPC по континенту, 700 Halisha's Mark с миньонов
Архона Халиши (Four Sepulchers / окрестности), бой с Архоном, засады. Награда: 3-я профессия, 5 000 000 адены,
Giant's Codex. Это групповой контент.

## 3. Зоны кача по уровням (данные сервера)

Как читать таблицу. Для каждого пункта телепорта гейткипера с названием посчитаны монстры в радиусе 4000 на том же
этаже: уровни (10-й–90-й перцентиль и медиана), доля агрессивных и самые частые мобы. `tp id` — id из
`teleportLocations.xml`, по нему бот может ехать гейткипером. Пересчитать: `python3 scripts/leveling_zones.py
<aCis_datapack>`. Зон без телепорта (подземелья с входом пешком, Imperial Tomb, Pagan Temple, Monastery of Silence,
катакомбы и некрополи Seven Signs) в таблице нет.

Правило выбора из классических гайдов: моб на 0–3 уровня выше персонажа даёт лучший опыт; при +5 и выше идут штрафы к
попаданию/урону, а агрессивные мобы на +6 — главная причина смертей ботов (уже учтено в `HuntingSpots.isDeadly`).
Зоны с долей агрессивных 60–100% — для пати, соло только сильным классам.


#### 1–10

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| The Orc Village (1012) | 1–3 (2) | 0% | 15 | Elder Prairie Keltir/3 (20538); Young Prairie Keltir/1 (20531); Prairie Keltir/2 (20535) |
| Elven Village (3) | 4–8 (5) | 0% | 10 | Goblin Raider/5 (20325); Kaboo Orc/6 (20468); Gray Wolf/4 (20525); Green Dryad/8 (20336) |
| Immortal Plateau (9105) | 7–8 (8) | 0% | 24 | Rakeclaw Imp Hunter/7 (20312); Mountain Fungus/8 (20365) |
| Obelisk of Victory (1001) | 5–10 (9) | 22% | 37 | Werewolf/9 (20132); Werewolf Hunter/10 (20343); Orc Archer/8 (20006); Orc Grunt/7 (20131) |
| The Center of the Dark Forest (9102) | 8–12 (10) | 13% | 15 | Goblin Thief/10 (20321); Blood Fungus/12 (20400); Goblin Lookout/8 (20328); Balor Orc Fighter/10 (20386) |

#### 10–20

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Dark Forest (464) | 10–12 (11) | 28% | 47 | Balor Orc Lieutenant/11 (20388); Balor Orc Fighter Leader/12 (20387); Blood Fungus/12 (20400); Goblin Thief/10 (20321) |
| Singing Waterfall (1004) | 9–12 (11) | 55% | 11 | Orc Lieutenant/11 (20096); Orc Fighter Leader/12 (20098); Werewolf/9 (20132); Werewolf Hunter/10 (20343) |
| Elven Forest (468) | 11–14 (12) | 42% | 40 | Kaboo Orc Fighter Leader/12 (20472); Kaboo Orc Fighter Lieutenant/11 (20473); Dryad/13 (20013); Dryad Elder/14 (20019) |
| Immortal Plateau, Northern Region (93) | 12–14 (13) | 0% | 9 | Greystone Golem/13 (20333); Goblin Tomb Raider Leader/12 (20320); Evil Eye Patroller/14 (20428) |
| Western Territory of Talking Island (Northern Area) (1002) | 13–14 (13) | 0% | 23 | Stone Golem/13 (20016); Crasher/14 (20101) |
| Abandoned Coal Mines (9107) | 10–17 (13) | 34% | 61 | Monster Eye Tracker/10 (20331); Akaste Bone Warlord/17 (20422); Opal Beast/15 (20435); Gemstone Beast/12 (20540) |
| Frozen Waterfall (96) | 13–16 (14) | 25% | 24 | Greystone Golem/13 (20333); Maraku Werewolf Chieftain/14 (20364); Evil Eye Patroller/14 (20428); Kasha Spider/15 (20474) |
| Swampland (466) | 14–15 (14) | 35% | 17 | Lesser Dark Horror/15 (20025); Stone Soldier/14 (20379); Marsh Zombie Lord/14 (20020); Festering Bat/13 (20433) |
| Western Mining Zone (Central Shore) (1022) | 12–17 (14) | 27% | 41 | Hunter Bear/14 (20777); Hunter Tarantula/16 (20403); Plunder Tarantula/17 (20508); Twink Puma/15 (20502) |
| Immortal Plateau, Southern Region (94) | 15–17 (16) | 36% | 14 | Kasha Fang Spider/16 (20476); Kasha Bear/15 (20479); Kasha Blade Spider/17 (20478) |
| Spider Nest (465) | 16–18 (17) | 42% | 59 | Prowler/17 (20034); Venomous Spider/18 (20038); Stone Guardian/16 (20380); Crystalline Beast/18 (20418) |
| Windmill Hill (1029) | 16–18 (17) | 73% | 22 | Vuku Orc Fighter/17 (20017); Brown Bear/16 (20310); Venomous Spider/18 (20038); Langk Lizardman/15 (20030) |
| Neutral Zone (9101) | 15–19 (17) | 60% | 10 | Arachnid Tracker/19 (20043); Moonstone Beast/17 (20369); Scavenger Spider/15 (20410); Scavenger Wererat/18 (20039) |
| Neutral Zone (1013) | 17–20 (18) | 47% | 15 | Moonstone Beast/17 (20369); Arachnid Tracker/19 (20043); Lycanthrope/20 (20049); Ol Mahum Sentry/17 (27031) |
| Evil Hunting Grounds (9100) | 17–20 (18) | 17% | 23 | Ratman Hunter/18 (20358); Ratman Chieftain/20 (21094); Ratman Sub Chieftain/19 (21093); Bugbear Raider/17 (20775) |
| Entrance to the Cave of Trials (471) | 18–25 (19) | 36% | 28 | Evil Eye Seer/18 (21257); Kasha Imp/19 (21117); Baar Dre Vanul/20 (21118); Baar Dre Vanul Destroyer/25 (21123) |
| Ruins of Agony (1042) | 17–22 (19) | 40% | 25 | Tracker Skeleton Leader/18 (20042); Skeleton Scout/19 (20045); Ruin Spartoi/21 (20054); Raging Spartoi/22 (20060) |
| Center of the School of Dark Arts (9103) | 14–22 (19) | 67% | 73 | Lesser Succubus Tilfo/22 (20057); Cave Spider/15 (20348); Misery Skeleton/14 (20022); Lesser Succubus Turen/21 (20052) |
| Elven Ruins (9104) | 9–22 (19) | 66% | 59 | Dre Vanul/20 (20117); Relic Werewolf/9 (20450); Dre Vanul Disposer/22 (20056); Skeleton/12 (20099) |

#### 20–30

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Eastern Mining Zone (Northeastern Shore) (17) | 18–23 (20) | 32% | 31 | Red Eye Barbed Bat/18 (21124); Kerope Werewolf/20 (21126); Kerope Werewolf Chief/23 (21129); Northern Trimden/19 (21125) |
| Abandoned Camp (90) | 15–24 (21) | 6% | 33 | Ol Mahum Patrol/21 (20053); Ol Mahum Guard/22 (20058); Ol Mahum Recruit/21 (20437); Cat's Eye Bandit/15 (27038) |
| Mithril Mines (419) | 19–21 (21) | 100% | 47 | Akaste Bone Lord/19 (20424); Nightmare Weaver/21 (20338); Akaste Succubus Turen/21 (20420) |
| Fellmere Harvesting Grounds (91) | 20–24 (22) | 54% | 24 | Giant Poison Bee/21 (21095); Cloudy Beast/22 (21096); Plain Grizzly/24 (21098); Arachnid Predator/20 (20050) |
| Langk Lizardman Dwelling (92) | 19–24 (22) | 46% | 24 | Langk Lizardman Lieutenant/22 (20357); Langk Lizardman Sentinel/19 (21100); Wandering Eye/21 (20055); Langk Lizardman Shaman/24 (21101) |
| Ruins of Despair (1043) | 19–23 (22) | 53% | 19 | Zombie Warrior/22 (20458); Veelan Bugbear/23 (20401); Zombie Soldier/19 (20457); Ruin Imp Elder/21 (20507) |
| Fortress of Resistance (86) | 23–26 (23) | 0% | 13 | Ol Mahum Reserve/23 (20547); Ol Mahum Shooter/24 (20063); Ol Mahum Guerilla/26 (20207); Ol Mahum Raider/27 (20208) |
| Plains of Dion (1053) | 23–26 (25) | 59% | 46 | Monster Eye Destroyer/26 (20068); Dire Wolf/24 (20205); Kadif Werewolf/25 (20206); Bloody Bee/23 (20204) |
| Crypts of Disgrace (9091) | 25–27 (25) | 0% | 9 | Grim Wolf/25 (22001); Grave Robber Scout/27 (22003) |
| Mithril Mines (9106) | 21–27 (26) | 46% | 116 | Underground Kobold Warrior/27 (21135); Dead Pit Spartoi/26 (21134); Dead Pit Skeleton Archer/25 (21133); Akaste Succubus Turen/21 (20420) |
| Windawood Manor (88) | 24–30 (27) | 41% | 27 | Ol Mahum Shooter/24 (20063); Ol Mahum Captain/25 (20066); Lesser Basilisk/27 (20070); Androscorpio/29 (20074) |
| Orc Barracks (1044) | 25–29 (27) | 20% | 35 | Turek Orc Sentinel/25 (20500); Turek Orc Archer/27 (20496); Turek Orc Footman/26 (20499); Turek Orc Skirmisher/28 (20497) |
| Wasteland (9011) | 25–33 (27) | 44% | 9 | Monster Eye Watcher/25 (20067); Magical Eye/31 (21012); Lesser Basilisk/27 (20070); Ol Mahum Van Archer/32 (21013) |
| The Ant Nest (1031) | 25–31 (29) | 62% | 26 | Ant Larva/29 (20075); Lesser Basilisk/27 (20070); Basilisk/28 (20072); Ant Captain/31 (20080) |
| Crypts of Disgrace (1119) | 20–31 (29) | 25% | 12 | Ghost of a Soldier/29 (22007); Grave Robber Scout/27 (22003); Grave Robber Ranger/28 (22005); Ghost of a Loyal Vassal/31 (22009) |
| Windy Hill (1121) | 28–30 (29) | 48% | 27 | Singing Wind/29 (21026); Giggling Wind/28 (21025); Whispering Wind/30 (20078) |
| Execution Grounds (9080) | 27–34 (29) | 52% | 48 | Neer Crawler/28 (20160); Neer Ghoul Berserker/29 (20198); Strain/31 (20200); Dead Seeker/34 (20202) |

#### 30–40

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Cruma Tower (1052) | 30–33 (31) | 13% | 53 | Marsh Stakato Worker/31 (20230); Toad Lord/32 (20231); Marsh Stakato Soldier/33 (20232); Stinger Wasp/30 (20229) |
| Breka's Stronghold (1062) | 30–32 (31) | 0% | 22 | Amber Basilisk/30 (20199); Breka Orc/31 (20267); Breka Orc Archer/32 (20268); Breka Orc Shaman/34 (20269) |
| Plunderous Plains (1117) | 30–37 (32) | 67% | 12 | Bandit Hound/31 (22018); Bandit Sweeper/30 (22017); Bandit Watchman/32 (22019); Snow Lynx/33 (22020) |
| Forgotten Temple (9010) | 31–33 (32) | 54% | 24 | Kanil Succubus/32 (20618); Rowin Undine/33 (20619); Roxide/32 (20747); Rakin Salamander/30 (20609) |
| Red Rock Ridge (1032) | 17–34 (33) | 100% | 8 | Lesser Warlike Tyrant/33 (21014); Langk Lizardman Warrior/17 (20024); Ol Mahum Van Leader/34 (21015); Langk Lizardman Sentinel/19 (21100) |
| Bee Hive (87) | 30–37 (34) | 58% | 60 | Hatu Dire Wolf/32 (20929); Hatu Brown Bear/34 (20930); Wasp Worker/35 (20934); Wasp Leader/37 (20935) |
| Gorgon Flower Garden (9040) | 33–35 (34) | 100% | 26 | Wyrm/35 (20176); Medusa/34 (20158); Turak Bugbear Warrior/33 (20249) |
| Floran Agricultural Area (9081) | 30–35 (35) | 46% | 24 | Cursed Seer/34 (21106); Delu Lizardman Shaman/35 (20781); Delu Lizardman Commander/35 (21107); Delu Lizardman Warrior/30 (20253) |
| Dragon Valley (69) | 35–46 (36) | 33% | 9 | Guardian Basilisk/36 (20550); Wyrm/35 (20176); Road Scavenger/37 (20551); Shackle/45 (20235) |
| Field of Silence (66) | 36–38 (37) | 38% | 63 | Tasaba Lizardman/36 (20784); Ogre/38 (21034); Tasaba Lizardman Shaman/37 (20785); Tasaba Lizardman/36 (21639) |
| Field of Whispers (9121) | 36–39 (37) | 33% | 9 | Dailaon/37 (20790); Crokian/36 (20789); Farhite/39 (20792); Crokian Warrior/38 (20791) |
| Field of Whispers (67) | 36–40 (38) | 38% | 32 | Dailaon/37 (20790); Farhite/39 (20792); Crokian/36 (20789); Nos/40 (20793) |
| Plains of the Lizardmen (97) | 35–40 (38) | 38% | 8 | Leto Lizardman Warrior/38 (20580); Leto Lizardman/35 (20577); Leto Lizardman Archer/36 (20578); Leto Lizardman Soldier/37 (20579) |

#### 40–52 (C)

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Sea of Spores (98) | 40–42 (41) | 31% | 13 | Giant Monstereye/41 (20556); Giant Fungus/40 (20555); Dire Wyrm/42 (20557) |
| Cruma Tower, 1st Floor (9083) | 40–42 (41) | 45% | 38 | Excuro/41 (20214); Catherok/42 (21035); Porta/40 (20213); Mordeo/42 (20215) |
| Garden of Eva, entrance (9123) | 40–42 (41) | 62% | 8 | Blade Stakato/41 (20794); Great White Shark/42 (20314); Nos/40 (20793); Light Worm/41 (20849) |
| Den of Evil (1116) | 40–45 (43) | 9% | 35 | Dark Spider/42 (22048); Guardian of the Ghost Town/44 (22047); Dark Corpse/40 (22045); Ragna Orc Archer/43 (22031) |
| Timak Outpost (9111) | 42–45 (43) | 20% | 10 | Timak Orc Warrior/43 (20586); Timak Orc Soldier/42 (20585); Timak Orc Shaman/44 (20587); Timak Orc Troop Leader/45 (20767) |
| Alligator Island (9122) | 41–45 (43) | 61% | 18 | Dailaon Lad/42 (20805); Crokian Lad Warrior/43 (20806); Crokian Lad/41 (20804); Nos Lad/45 (20808) |
| Forest of Mirrors (101) | 43–45 (44) | 71% | 14 | Taik Orc Captain/44 (20634); Taik Orc Shaman/43 (20633); Carinkain/45 (20635); Forest of Mirrors Ghost/46 (20636) |
| Devil's Isle (1061) | 43–46 (44) | 57% | 42 | Zaken's Pikeman/43 (20832); Pirate Zombie/44 (20836); Unpleasant Humming/46 (20839); Zaken's Archer/44 (20833) |
| Hunters Valley (9028) | 41–45 (44) | 57% | 14 | Kronbe Spider/43 (20603); Weird Drake/45 (20605); Lakin/44 (20604); Karul Bugbear/40 (20600) |
| Garden of Eva, 2nd level (9124) | 40–46 (44) | 52% | 33 | Water Giant/45 (20798); Blade Stakato Warrior/43 (20796); Blade Stakato Drone/44 (20797); Queen Undine Lad/46 (20799) |
| Garden of Eva, 3rd level (9125) | 41–48 (45) | 40% | 48 | Eva's Seeker/47 (20800); Blade Stakato/41 (20794); Blade Stakato Drone/44 (20797); Water Giant/45 (20798) |
| Plains of Glory (9021) | 45–47 (46) | 40% | 10 | Vanor Silenos Grunt/46 (20682); Vanor Silenos Scout/47 (20683); Vanor Silenos/45 (20681) |
| Tanor Canyon (1054) | 40–50 (47) | 69% | 13 | Tanor Silenos Chieftain/50 (20941); Tanor Silenos/40 (20936); Nightmare Lord/47 (20944) |
| Pavel Ruins (1120) | 47–48 (47) | 43% | 21 | Spelunking Spider/47 (22051); Menacing Jackhammer Golem/47 (22052); Probe Golem Prodigy IV/46 (22050); Horrifying Cannon Golem IV/48 (22054) |
| Cruma Tower, 2nd Floor (9084) | 46–49 (47) | 56% | 18 | Validus/46 (20219); Dicor/47 (20220); Ossiud/47 (21037); Torfe/49 (20222) |
| Forest of Evil (9110) | 46–48 (47) | 26% | 19 | Tarlk Bugbear/47 (20570); Formor Elder/46 (20569); Tarlk Bugbear Warrior/48 (20571); Formor/45 (20568) |
| Garden of Eva, 4th level (9126) | 42–50 (47) | 48% | 73 | Eva's Seeker/47 (20800); Theeder Piker/48 (20801); Water Giant/45 (20798); Theeder Mage/49 (20802) |
| War-Torn Plains (9022) | 48–50 (49) | 37% | 19 | Hatar Hanishee/49 (20663); Deprive/50 (20664); Hatar Ratman Boss/48 (20662) |
| Garden of Eva, 5th level (9127) | 48–50 (49) | 65% | 80 | Theeder Mage/49 (20802); Doll Master/50 (20803); Garden Guard Leader/50 (20994); Theeder Piker/48 (20801) |
| Outlaw Forest (1077) | 50–53 (51) | 40% | 35 | Hunter Gargoyle/52 (20241); Elder Tarlk Basilisk/51 (20574); Tarlk Basilisk/50 (20573); Oel Mahum/53 (20161) |

#### 52–61 (B)

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Enchanted Valley (9031) | 48–54 (52) | 25% | 20 | Liele Elder/52 (20596); Satyr Elder/54 (20598); Forest Runner/50 (20594); Fline Elder/51 (20595) |
| Forest of Mirrors (9030) | 51–55 (53) | 36% | 14 | Harit Lizardman Warrior/53 (20643); Harit Lizardman Shaman/54 (20644); Harit Lizardman Matriarch/55 (20645); Harit Lizardman Grunt/51 (20641) |
| Skyshadow Meadow (1076) | 54–54 (54) | 5% | 19 | Connabi/54 (20947); Pobby/57 (20953) |
| Forsaken Plains (84) | 54–58 (55) | 30% | 10 | Marsh Stalker/54 (20679); Marsh Drake/55 (20680); Fallen Orc/55 (21017); Ancient Gargoyle/56 (21018) |
| Fields of Massacre (1085) | 55–57 (56) | 26% | 23 | Grave Lich/57 (21003); Wretched Archer/55 (21001); Doom Scout/56 (21002); Doom Knight/65 (20674) |
| Dragon Valley, Traders Post (9045) | 49–59 (58) | 72% | 43 | Cave Maiden/59 (20134); Cave Keeper/58 (20246); Drake/57 (20137); Dragon Bearer Chief/49 (20758) |

#### 61–76 (A)

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Antharas Lair (1060) | 59–62 (61) | 100% | 20 | Death Wave/62 (20621); Cave Beast/61 (20620); Bloody Lady/60 (21084); Blood Queen/60 (18001) |
| Valley of Saints (1104) | 60–62 (61) | 54% | 26 | Flash of Splendor/61 (21523); Blade of Splendor/61 (21524); Wisdom of Splendor/62 (21526); Eye of Splendor/60 (21520) |
| Forest of the Dead (124) | 63–68 (64) | 31% | 175 | Corpse of Deadman/63 (18119); Skull Collector/67 (21564); Corrupted Knight/63 (21547); Bone Collector/67 (21563) |
| Seal of Shilen (1083) | 60–65 (64) | 75% | 44 | Falibati/64 (20673); Trives/63 (20672); Doom Knight/65 (20674); Blood Queen/60 (18001) |
| Wild Beast Pastures (1103) | 63–68 (64) | 100% | 11 | Alpine Bandersnatch/64 (21445); Raider of Pastureland/63 (21444); Raider of Pastureland/64 (21446); Raider of Pastureland/68 (21449) |
| Tower of Insolence, 3rd Floor (9023) | 63–66 (64) | 100% | 18 | Hallate's Royal Guard/63 (20816); Corrupt Sage/64 (20817); Hallate's Knight/65 (20820); Hallate's Maid/66 (20822) |
| Valley of Saints (9051) | 61–65 (64) | 38% | 37 | Punishment of Splendor/64 (21531); Shout of Splendor/65 (21532); Alliance of Splendor/65 (21533); Flash of Splendor/61 (21523) |
| Antharas Lair 1 (9041) | 63–68 (66) | 100% | 113 | Malruk Knight/66 (20625); Malruk Lord/68 (20627); Malruk Berserker/67 (20626); Cave Howler/65 (20624) |
| Antharas Lair, Magic Force Field Bridge (9043) | 64–75 (67) | 100% | 95 | Bloody Lord/75 (21089); Cave Howler/65 (20624); Bloody Knight/73 (21087); Malruk Knight/66 (20625) |
| Stakato Nest (9053) | 66–69 (67) | 100% | 11 | Splinter Stakato/66 (21508); Splinter Stakato Worker/67 (21509); Splinter Stakato Soldier/68 (21510); Splinter Stakato Drone/69 (21511) |
| Swamp of Screams (125) | 66–69 (68) | 95% | 19 | Splinter Stakato Worker/67 (21509); Splinter Stakato Soldier/68 (21510); Splinter Stakato Drone/69 (21511); Splinter Stakato/66 (21508) |
| Tower of Insolence, 5th Floor (9024) | 67–69 (68) | 100% | 49 | Hallate's Commander/67 (20824); Hallate's Guardian/69 (21061); Platinum Tribe Soldier/67 (20823); Hallate's Inspector/68 (20825) |
| Blazing Swamp (81) | 67–72 (69) | 11% | 18 | Glow Wisp/67 (21108); Hames Orc Scout/68 (21109); Hames Orc Footman/71 (21112); Marsh Predator/69 (21110) |
| Ancient Battleground (1086) | 68–70 (70) | 100% | 11 | Dark Guard/70 (20959); Bloody Lord/68 (20963) |
| Tower of Insolence, 7th Floor (9025) | 67–72 (70) | 100% | 45 | Platinum Tribe Soldier/67 (20823); Platinum Tribe Archer/68 (20826); Messenger Angel/70 (21062); Platinum Tribe Warrior/69 (20827) |
| Antharas Lair 2 (9042) | 69–72 (70) | 100% | 107 | Karik/70 (20629); Limal Karinness/69 (20628); Bloody Sniper/71 (21085); Bloody Liviona/72 (21086) |
| Garden of Beasts (9063) | 69–70 (70) | 41% | 17 | Bandersnatch/69 (21285); Buffalo/70 (21287); Buffalo/70 (21288); Bandersnatch/69 (21283) |
| Forest of the Dead (9050) | 69–73 (72) | 31% | 93 | Ghost of Guillotine/70 (21576); Vampire Soldier/71 (21582); Bone Grinder/70 (21575); Bone Scavenger/72 (21583) |
| Cursed Village (9052) | 63–73 (72) | 27% | 60 | Corpse of Deadman/63 (18119); Vampire Adept/72 (21586); Vampire Magician/72 (21585); Vampire Wizard/73 (21589) |
| Hot Springs (1095) | 73–75 (74) | 21% | 38 | Hot Springs Buffalo/73 (21315); Hot Springs Antelope/74 (21318); Hot Springs Yeti/75 (21320); Hot Springs Bandersnatchling/73 (21314) |
| Tower of Insolence, 10th Floor (9026) | 72–75 (74) | 100% | 38 | Platinum Guardian Prefect/74 (21069); Platinum Guardian Shaman/73 (21066); Guardian Angel/72 (20830); Guardian Archangel/74 (21067) |
| Tower of Insolence, 13th Floor (9027) | 73–75 (74) | 100% | 15 | Guardian Archangel/74 (21067); Platinum Guardian Shaman/73 (21066); Platinum Guardian Chief/75 (21072) |
| Hot Springs (9064) | 73–76 (74) | 69% | 13 | Hot Springs Bandersnatchling/73 (21314); Hot Springs Antelope/74 (21318); Hot Springs Bandersnatch/76 (21322); Hot Springs Nepenthes/75 (21319) |
| Antharas Lair, Heart of Warding (9044) | 73–75 (75) | 100% | 59 | Bloody Lord/75 (21089); Bloody Knight/73 (21087); Bloody Banshee/74 (21088); Bloody Guardian/75 (21090) |
| Wall of Argos (9065) | 74–75 (75) | 100% | 8 | Disciples of Protection/74 (21306); Elder Homunculus/75 (21307); Disciples of Punishment/75 (21309) |

#### 76–80 (S)

| Зона (tp id) | Ур. мобов p10–p90 (медиана) | Агрессивных | Мобов | Основные мобы (id) |
|---|---|---|---|---|
| Varka Silenos Stronghold (109) | 77–79 (77) | 100% | 70 | Grazing Antelope/77 (21352); Varka Silenos Recruit/77 (21350); Varka Silenos Footman/77 (21351); Varka Silenos Hunter/78 (21354) |
| Varka Silenos Village (9060) | 80–80 (80) | 100% | 10 | Grazing Elder Antelope/80 (21367); Varka Silenos Great Seer/80 (21368); Varka Silenos Great Magus/80 (21365); Varka Silenos General/80 (21366) |
| Ketra Orc Village (9061) | 79–80 (80) | 100% | 44 | Ketra Orc General/80 (21339); Ketra Orc Battalion Commander/80 (21340); Ketra Orc Lieutenant/80 (21332); Grazing Elder Kookaburra/80 (21341) |
| Four Sepulchers (9067) | 80–80 (80) | 75% | 8 | Ashkenas/80 (21424); Ashkenas/80 (21425); Abraxion/80 (21426); Abraxion/80 (21427) |
| Forge of the Gods, lower level (9071) | 80–80 (80) | 100% | 65 | Iblis of Destruction/80 (21388); Lavasillisk/80 (21391); Blazing Ifrit/80 (21392); Ashuras of Destruction/80 (21390) |

### 3.1. Маршрут по классическим гайдам, сверенный с таблицей

| Уровень | Соло-воины (рыцари, воины, гномы) | Маги-нюкеры и саммонеры | Лучники/кинжалы | Пати |
|---|---|---|---|---|
| 1–10 | своя деревня новичков | то же | то же | — |
| 10–20 | Elven Forest (468), Dark Forest (464), Swampland (466), Western Mining Zone (1022), Evil Hunting Grounds (9100) | те же; маги — Ruins of Agony (1042) нежить | Neutral Zone (1013), Spider Nest (465) | Elven Ruins (9104), School of Dark Arts (9103) |
| 20–30 | Ruins of Despair (1043), Abandoned Camp (90), Orc Barracks (1044), Windawood Manor (88) | Fellmere (91), Plains of Dion (1053), Crypts of Disgrace (9091) | Langk Lizardmen (92), Wasteland (9011) | Ant Nest (1031), Execution Grounds (9080) |
| 30–40 | Cruma Marshlands (1052), Breka's Stronghold (1062), Forgotten Temple (9010) | Floran Agricultural Area (9081), Field of Silence/Whispers (66/67) | Bee Hive (87), Plains of the Lizardmen (97) | Gorgon Flower Garden (9040), Dragon Valley (69), Execution Grounds |
| 40–52 | Sea of Spores (98), Timak Outpost (9111), Hunters Valley (9028), Forest of Evil (9110) | Cruma Tower 1–2 (9083/9084), Plains of Glory (9021) | Alligator Island (9122), Den of Evil (1116), Pavel Ruins (1120) | Garden of Eva (9123–9127), Devil's Isle (1061), Tanor Canyon (1054) |
| 52–61 | Outlaw Forest (1077), Enchanted Valley (9031) | Forest of Mirrors (9030), Skyshadow Meadow (1076) | Forsaken Plains (84) | Fields of Massacre (1085), Dragon Valley (9045) |
| 61–76 | Valley of Saints (1104/9051), Wild Beast Pastures (1103) | Forest of the Dead (124/9050), Cursed Village (9052) | Garden of Beasts (9063), Blazing Swamp (81) | Swamp of Screams (125), Stakato Nest (9053), Tower of Insolence (9023–9027), Antharas Lair (9041/9042/9044), Hot Springs (1095/9064) |
| 76–80 | Varka/Ketra окрестности (109) | Hot Springs, Forge of the Gods (9071) | то же | Varka (9060)/Ketra (9061), Four Sepulchers (9067), Imperial Tomb, Pagan Temple |

Колонки по архетипам — общепринятые советы гайдов, по данным сервера проверены только уровни и агрессивность зон, но
не то, насколько зона удобна именно магу или лучнику.

## 4. Экипировка по грейдам

Грейд без штрафа по уровню: NG до 19, D 20–39, C 40–51, B 52–60, A 61–75, S 76+ (так же считает `Gear.gradeFor`).
Сеты сервера (`armorSets.xml`, цена — справочная из `items`, «у NPC» — есть в `buyLists.xml`). Клановые наборы Clan
Oath/Apella — награды клану, не для обычной прокачки.

| Грейд | Вид | Сет | Части (id) | Цена | У NPC |
|---|---|---|---|---|---|
| NONE | LIGHT | Wooden Set | chest 23, legs 2386, head 43, gloves —, feet — | 7960 | да |
| NONE | MAGIC | Devotion Set | chest 1101, legs 1104, head 44, gloves —, feet — | 20300 | да |
| D | HEAVY | Brigandine Set | chest 352, legs 2378, head 2411, gloves —, feet — | 299000 | да |
| D | HEAVY | Mithril Robe Set | chest 58, legs 59, head 47, gloves —, feet — | 183000 | да |
| D | LIGHT | Manticore Skin Set | chest 395, legs 417, head 0, gloves —, feet 2424 | 224000 | да |
| D | LIGHT | Reinforced Leather Set | chest 394, legs 416, head 0, gloves —, feet 2422 | 152000 | да |
| D | MAGIC | Knowledge Set | chest 436, legs 469, head 0, gloves 2447, feet — | 152000 | да |
| D | MAGIC | Mithril Robe Set | chest 437, legs 470, head 0, gloves 2450, feet — | 224000 | да |
| C | HEAVY | Chain Mail Set | chest 354, legs 381, head 2413, gloves —, feet — | 505000 | да |
| C | HEAVY | Composite Set | chest 60, legs —, head 517, gloves —, feet — | 1440000 | да |
| C | HEAVY | Full Plate Set | chest 356, legs —, head 2414, gloves —, feet — | 2090000 | да |
| C | LIGHT | Drake Leather Set | chest 401, legs —, head 0, gloves —, feet 2437 | 1570000 | да |
| C | LIGHT | Mithril Light Set | chest 397, legs 2387, head 0, gloves —, feet 62 | 379000 | да |
| C | LIGHT | Plated Leather Set | chest 398, legs 418, head 0, gloves —, feet 2431 | 446000 | да |
| C | LIGHT | Theca Leather Set | chest 400, legs 420, head 0, gloves —, feet 2436 | 825000 | да |
| C | MAGIC | Demon Set | chest 441, legs 472, head 0, gloves 2459, feet — | 736000 | да |
| C | MAGIC | Divine Set | chest 442, legs 473, head 0, gloves 2463, feet — | 1070000 | да |
| C | MAGIC | Karmian Set | chest 439, legs 471, head 0, gloves 2454, feet — | 379000 | да |
| B | HEAVY | Avadon Heavy Set | chest 2376, legs 2379, head 2415, gloves 5714, feet 5730 | 2080000 | да |
| B | HEAVY | Blue Wolf Heavy Set | chest 358, legs 2380, head 2416, gloves 5718, feet 5734 | 3220000 | да |
| B | HEAVY | Doom Heavy Set | chest 2381, legs —, head 2417, gloves 5722, feet 5738 | 4710000 | да |
| B | HEAVY | Zubei Heavy Set | chest 357, legs 383, head 503, gloves 5710, feet 5726 | 2080000 | да |
| B | LIGHT | Avadon Light Set | chest 2390, legs —, head 2415, gloves 5715, feet 5731 | 2280000 | да |
| B | LIGHT | Blue Wolf Light Set | chest 2391, legs —, head 2416, gloves 5719, feet 5735 | 3530000 | да |
| B | LIGHT | Doom Light Set | chest 2392, legs —, head 2417, gloves 5723, feet 5739 | 3530000 | да |
| B | LIGHT | Zubei Light Set | chest 2384, legs 2388, head 503, gloves 5711, feet 5727 | 1560000 | да |
| B | MAGIC | Avadon Robe Set | chest 2406, legs —, head 2415, gloves 5716, feet 5732 | 2280000 | да |
| B | MAGIC | Blue Wolf Robe Set | chest 2398, legs 2403, head 2416, gloves 5720, feet 5736 | 2410000 | да |
| B | MAGIC | Doom Robe Set | chest 2399, legs 2404, head 2417, gloves 5724, feet 5740 | 2410000 | да |
| B | MAGIC | Zubei Robe Set | chest 2397, legs 2402, head 503, gloves 5712, feet 5728 | 1560000 | да |
| A | HEAVY | Dark Crystal Heavy Set | chest 365, legs 388, head 512, gloves 5765, feet 5777 | 4630000 | да |
| A | HEAVY | Majestic Heavy Set | chest 2383, legs —, head 2419, gloves 5774, feet 5786 | 10300000 | да |
| A | HEAVY | Nightmare Heavy Set | chest 374, legs —, head 2418, gloves 5771, feet 5783 | 10300000 | да |
| A | HEAVY | Tallum Heavy Set | chest 2382, legs —, head 547, gloves 5768, feet 5780 | 6770000 | да |
| A | LIGHT | Dark Crystal Light Set | chest 2385, legs 2389, head 512, gloves 5766, feet 5778 | 3470000 | да |
| A | LIGHT | Majestic Leather Set | chest 2395, legs —, head 2419, gloves 5775, feet 5787 | 7730000 | да |
| A | LIGHT | Nightmare Light Set | chest 2394, legs —, head 2418, gloves 5772, feet 5784 | 7730000 | да |
| A | LIGHT | Tallum Light Set | chest 2393, legs —, head 547, gloves 5769, feet 5781 | 5080000 | да |
| A | MAGIC | Dark Crystal Robe Set | chest 2407, legs —, head 512, gloves 5767, feet 5779 | 5080000 | да |
| A | MAGIC | Majestic Robe Set | chest 2409, legs —, head 2419, gloves 5776, feet 5788 | 7730000 | да |
| A | MAGIC | Nightmare Robe Set | chest 2408, legs —, head 2418, gloves 5773, feet 5785 | 7730000 | да |
| A | MAGIC | Tallum Robe Set | chest 2400, legs 2405, head 547, gloves 5770, feet 5782 | 3470000 | да |
| S | HEAVY | Imperial Crusader Set | chest 6373, legs 6374, head 6378, gloves 6375, feet 6376 | 14300000 | да |
| S | LIGHT | Draconic Leather Set | chest 6379, legs —, head 6382, gloves 6380, feet 6381 | 17400000 | да |
| S | MAGIC | Major Arcana Set | chest 6383, legs —, head 6386, gloves 6384, feet 6385 | 17400000 | да |

Общепринятый выбор по гайдам Interlude:

- **D (20–39):** тяжёлая — Brigandine → Mithril (Mithril Breastplate 58); лёгкая — Reinforced Leather → Manticore Skin;
  роба — Knowledge → Mithril Robe (Tunic 437). Оружие D: мечи/дубины, у магов Goat Head Staff / Cedar Staff.
- **C (40–51):** тяжёлая — Chain Mail → Composite / Full Plate; лёгкая — Plated Leather / Theca / Drake; роба —
  Karmian / Demon's / Divine. Оружие с SA (Q350 + кристаллы душ) — стандарт для C+.
- **B (52–60):** тяжёлая — Blue Wolf / Doom (Zubei/Avadon дешевле); лёгкая — Blue Wolf / Doom; роба — Avadon / Doom.
- **A (61–75):** тяжёлая — Dark Crystal / Nightmare / Tallum / Majestic; лёгкая — Dark Crystal / Majestic / Nightmare;
  роба — Dark Crystal / Majestic / Nightmare / Tallum.
- **S (76+):** Imperial Crusader (тяжёлая), Draconic Leather (лёгкая), Major Arcana (роба).
- Тип брони по классу: рыцари, воины, деструеры, гномы — тяжёлая; кинжалы, лучники, тираны — лёгкая; маги, хилы,
  баферы, саммонеры — роба. Щит — только с одноручным оружием.

## 5. Навыки и книги

Население учит навыки у тренера за SP и книгу (`SkillLearner`). Книга нужна только на 1-й уровень навыка.
Почти все книги 1–76 продаются у NPC. Исключения, которые бот не купит в магазине (только дроп, крафт, квест или
лавка игрока), — по данным `spellbooks.xml` против `buyLists.xml`:

| Класс | Книг всего | У NPC | Нет у NPC (уровень навыка) |
|---|---|---|---|
| Shillien Templar | 1 | 0 | Ancient Tactical Manual: Symbol of Defense (80) |
| Spectral Dancer | 1 | 0 | Ancient Tactical Manual: Symbol of Noise (80) |
| Ghost Hunter | 1 | 0 | Ancient Tactical Manual: Symbol of the Assassin (80) |
| Ghost Sentinel | 1 | 0 | Ancient Tactical Manual: Symbol of the Sniper (80) |
| Storm Screamer | 4 | 3 | Ancient Spellbook: Cyclone (80) |
| Spectral Master | 5 | 3 | Spellbook: Summon Spectral Knight (79); Ancient Spellbook: Anti-Summoning Field (80) |
| Shillen Saint | 4 | 3 | Ancient Spellbook: Mass Recharge (80) |
| Maestro | 1 | 0 | Ancient Tactical Manual: Symbol of Honor (80) |
| Fortune Seeker | 1 | 0 | Ancient Tactical Manual: Symbol of Resistance (80) |
| Eva's Templar | 1 | 0 | Ancient Tactical Manual: Symbol of Defense (80) |
| Sword Muse | 1 | 0 | Ancient Tactical Manual: Symbol of Noise (80) |
| Wind Rider | 1 | 0 | Ancient Tactical Manual: Symbol of the Assassin (80) |
| Moonlight Sentinel | 1 | 0 | Ancient Tactical Manual: Symbol of the Sniper (80) |
| Spellsinger | 20 | 18 | Spellbook: Freezing Shackle (44); Spellbook: Frost Wall (52) |
| Mystic Muse | 4 | 3 | Ancient Spellbook: Raging Waves (80) |
| Elemental Master | 5 | 3 | Spellbook: Summon Magnus the Unicorn (79); Ancient Spellbook: Anti-Summoning Field (80) |
| Eva's Saint | 5 | 4 | Ancient Spellbook: Mass Recharge (80) |
| Duelist | 1 | 0 | Ancient Tactical Manual: Symbol of Energy (80) |
| Dreadnought | 1 | 0 | Ancient Tactical Manual: Symbol of Honor (80) |
| Phoenix Knight | 2 | 0 | Spellbook: Soul of the Phoenix (79); Ancient Tactical Manual: Symbol of Defense (80) |
| Hell Knight | 1 | 0 | Ancient Tactical Manual: Symbol of Defense (80) |
| Sagittarius | 1 | 0 | Ancient Tactical Manual: Symbol of the Sniper (80) |
| Adventurer | 1 | 0 | Ancient Tactical Manual: Symbol of the Assassin (80) |
| Bishop | 24 | 22 | Spellbook: Body of Avatar (48); Spellbook: Prayer (66) |
| Archmage | 3 | 2 | Ancient Spellbook: Volcano (80) |
| Soultaker | 7 | 5 | Ancient Spellbook: Day of Doom (80); Ancient Spellbook: Gehenna (80) |
| Arcana Lord | 6 | 4 | Spellbook: Summon Feline King (79); Ancient Spellbook: Anti-Summoning Field (80) |
| Cardinal | 8 | 4 | Spellbook: Cleanse (78); Spellbook: Salvation (79); Ancient Spellbook: Purification Field (80); Ancient Spellbook: Miracle (80) |
| Hierophant | 6 | 4 | Spellbook: Mystic Immunity (79); Spellbook: Spell Turning (79) |
| Titan | 1 | 0 | Ancient Tactical Manual: Symbol of Resistance (80) |
| Grand Khavatari | 1 | 0 | Ancient Tactical Manual: Symbol of Energy (80) |
| Overlord | 19 | 17 | Amulet: Ritual of Life (64); Amulet: Pa'agrio's Honor (66) |
| Warcryer | 11 | 8 | Amulet: Chant of Vampire (44); Amulet: Chant of Predator (48); Amulet: Chant of Eagle (48) |
| Dominator | 8 | 4 | Amulet: Pa'agrio's Emblem (78); Amulet: Victories of Pa'agrio (79); Amulet: Pa'agrio's Fist (79); Ancient Spellbook: Flames of Invincibility (80) |

Все «Ancient Tactical Manual / Ancient Spellbook» на 80 и книги 78–79 уровня — дроп с эпиков/рейдов и из квестов,
это нормально и для игроков. Важные для прокачки дыры раньше 76: **Spellsinger** (Freezing Shackle 44, Frost Wall 52),
**Bishop** (Body of Avatar 48, Prayer 66), **Warcryer** (Chant of Vampire 44, Predator/Eagle 48), **Overlord** (Ritual of
Life 64, Pa'agrio's Honor 66).

## 6. Квесты для прокачки, кроме профессий

- Уже у ботов (`RepeatQuests.kt`): 10 повторяемых «сдай добычу» с 6 до 42 уровня.
- Есть в aCis для 38+ и подходят как «подработка» на зонах из раздела 3: Q338 Alligator Hunter (40, Alligator
  Island), Q354 Conquest of Alligator Island (38), Q356 Dig Up the Sea of Spores (43), Q647 Influx of Machines (46),
  Q360 Plunder Their Supplies (52), Q648 An Ice Merchant's Dream (53), Q371 Shriek of Ghosts (59), Q359 For Sleepless
  Deadmen (60), Q633 In the Forgotten Village (65), Q629 Clean Up the Swamp of Screams (66), Q628 Hunt of the Golden Ram
  (66), Q602 Shadow of Light (68), Q601 Watching Eyes (71), Q605–Q616 альянсы Кетры и Варки (74–80), Q619 Relics of the
  Old Empire (74), Q620 Four Goblets.
- Q350 Enhance Your Weapon (40) — прокачка кристаллов душ для SA, ключевой для C+ оружия.

## 7. Расхождения с тем, что делают боты сейчас

Проверено по коду L2Autobots с применёнными патчами (сборка по `acis/Dockerfile`).

| # | Что в гайде | Что у ботов | Где | Важность |
|---|---|---|---|---|
| 1 | 2-я профессия на 40 через три марки, 3-я на 76 через сагу | Население меняет только 1-ю профессию (Path-квесты). Бесплатная смена выключена (`onLevelChange` выходит при `questsEnabled`), марок и саг нет, поэтому бот 40+ навсегда остаётся Warrior/Knight/… | `Autobot.onLevelChange`, `PathQuests.kt`, `docs/quests.md` | высокая |
| 2 | На 40+ C-грейд и дальше | В `equipment.xml` у 1-х профессий строки только 20–39. `Gear.wishes` ищет строку точно по уровню, не находит и возвращает пустой список: житель 40+ перестаёт ставить цели покупки экипировки вообще (вид брони при этом берётся из ближайшей нижней строки, #110). Из-за п.1 это касается всех жителей 40+ | `data/autobots/equipment.xml`, `Gear.wishes` | высокая; **исправлено** в `l2autobots-guide-gear.patch` |
| 3 | Монк — свой набор (кастеты, лёгкая) | У `MONK` нет ни одной строки в `equipment.xml`: с 20 уровня монк-житель ничего из экипировки не покупает | `equipment.xml` | высокая для орков-монков; **исправлено** в `l2autobots-guide-gear.patch` |
| 4 | Маг 20–39 в D-робе (Knowledge/Mithril Robe) | Цель — Tunic/Stockings of Devotion и Leather Helmet (No-grade) | `equipment.xml` (HUMAN_WIZARD … ORC_SHAMAN) | средняя (планировщик брони офлайн-персонажа берёт грейд по уровню, а набор населения — нет); **исправлено** в `l2autobots-guide-gear.patch` |
| 5 | Маг 76+ в Major Arcana | Цель — Dark Crystal Robe (A) + Imperial Crusader Shield (тяжёлый S-щит) | `equipment.xml` (ARCHMAGE … EVAS_SAINT) | средняя; **исправлено** в `l2autobots-guide-gear.patch` |
| 6 | Щит только с одноручным | Spectral Dancer 76: дуалы + Imperial Crusader Shield; Titan 76: Heavens Divider (двуручный) + щит — невозможные комбинации | `equipment.xml` | низкая (щит просто не наденется); **исправлено** в `l2autobots-guide-gear.patch` |
| 7 | Маг/лучник/кинжал 40–51 в шлеме своего сета | Full Plate Helmet (тяжёлый) у всех | `equipment.xml` | низкая |
| 8 | Зона по уровню **и** роли (маги — нежить/кайт, воины — соло-мобы, пати — агрессивные гнёзда), миграция по регионам | Спот — ячейка 2500×2500 с ≥4 мобами уровня −4…+2, вес падает с расстоянием (`exp(-d/10000)`), класс и роль не учитываются, названий зон нет | `HuntingSpots.choose/score` | средняя: проверить на живом сервере, куда реально ходят 20–40 |
| 9 | Купоны теневого оружия D (15 шт. за 1-ю профессию) меняют у NPC | Боты купоны не обменивают (в коде нет обработки купонов) | `population/economy`, `QuestRunner.changeClass` | средняя (деньги на старте 20–25) |
| 10 | Повторяемые квесты на всех уровнях | 10 квестов, последний до 42 ур. | `RepeatQuests.kt` | низкая/средняя |
| 11 | Книги, которых нет у NPC (Spellsinger 44/52, Bishop 48/66, Warcryer 44/48, Overlord 64/66) | `SkillLearner` ищет в магазине и лавках; при п.1 до этих уровней население не доходит в нужном классе | `SkillLearner` | станет важной после п.1 |
| 12 | Рыцари в PvE используют Power Strike/Drain Health и т.п. | Ветка рыцаря бьёт автоатакой (известно из PROGRESS, `fix/knight-skills` не принят) | профили навыков | средняя, уже в работе |
| 13 | Q350 и SA на оружии C+ | Не реализовано (кристаллы душ и вставка SA ботам неизвестны) | — | низкая |

Что совпадает с гайдами: грейды по уровням (`Gear.gradeFor`), тип брони по набору класса (#110), навыки у тренера за
SP+книги, Path-квесты всех 18 первых профессий, 10 повторяемых квестов новичковых зон, ветки 89 классов с
поведениями (у каждого `ClassId` есть боевое поведение), Death Penalty и снабжение.

## 8. План (на одобрение, без изменений поведения до него)

1. **Данные экипировки — сделано** (`l2autobots-guide-gear.patch`, проверка `scripts/check_equipment.py`). Исправить `equipment.xml`: строки 40–80 для 1-х профессий (временный
   набор C/B/A/S по виду брони ветки, пока нет 2-й профессии), добавить `MONK`, D-робу для магов 20–39, Major Arcana для
   магов 76+, убрать щит у Spectral Dancer и Titan. Проверка — существующий `ArmorKindVerify` плюс выборочная проверка
   набора на 45/55/65/78.
2. **2-я профессия.** Сценарии 23 марочных квестов в общем исполнителе квестов (как Path-квесты), смена у мастера
   гильдии через `SecondClassChange`. Начать с самых массовых веток (Q211 Challenger, Q212 Duty, Q213 Seeker, Q214
   Scholar, Q215 Pilgrim, Q217 Trust, Q219 Fate). Это крупный этап, отдельная спецификация.
3. **Выбор зоны по роли.** Таблица раздела 3 как «предпочтительные зоны» по диапазону уровней и архетипу (бонус к
   весу спота, а не жёсткий фильтр), чтобы боты мигрировали по регионам, как игроки. Сначала замер: куда реально ходят
   боты 20–40 сейчас.
4. **Купоны теневого оружия** после 1-й профессии и **повторяемые квесты 40–75** из раздела 6.
5. **Саги и Q350** — после групп и 2-й профессии.

## Источники

- Код и данные сервера: aCis 382 `452cd109` (`spawnlist.sql`, `data/xml/npcs`, `teleportLocations.xml`,
  `html/teleporter`, `armorSets.xml`, `buyLists.xml`, `spellbooks.xml`, `classes/*.xml`, `SecondClassChange.java`,
  квесты Q211–Q233, Q401–Q418, Q070–Q100), L2Autobots `f990c61d` с патчами репозитория.
- Описание локаций: [StrategyWiki — Execution Grounds](https://strategywiki.org/wiki/Lineage_II/Notable_Locations/Execution_Grounds),
  [l2hub.info — Ant Nest](https://l2hub.info/c3/locs/ant-nest) (C3, уровни отличаются от IL — использовались только как
  подтверждение состава мобов).
- Отвергнуто как недостоверное: l2db.net (`l2-leveling-guide-1-75`, `best-solo-grind-spots-il`) — выдуманные зоны и
  квесты, не относящиеся к Interlude.
