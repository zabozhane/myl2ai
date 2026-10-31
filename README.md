# l2aiserver

Локальный приватный сервер Lineage 2 Interlude (C6, протокол 746) на aCis с ботами. Всё серверное работает в Docker (OrbStack), Java на Mac не ставится.

Текущий этап: **1. MariaDB + сборка и запуск чистого aCis 382.**

## Схема этапа 1

```
Mac (winemac, клиент C6 / 746)
  │ 127.0.0.1:2106                │ 127.0.0.1:7777
  ▼                               ▼
┌──────────────┐  9014 (внутр.) ┌──────────────┐
│ login        │◄──────────────►│ game         │
│ aCis 382     │ регистрация GS │ aCis 382     │
│ JRE 11, 64 МБ│                │ JRE 11, 2 ГБ │
└──────┬───────┘                └──────┬───────┘
       │ схема l2_login                │ схема l2_game
       ▼                               ▼
┌─────────────────────────────────────────────┐
│ db: MariaDB 11.4 (том db-data)              │
└─────────────────────────────────────────────┘
   ▲ первый старт: db-init заливает SQL aCis в пустые схемы
```

- **Образ `l2aiserver/acis:382`** собирается multi-stage: JDK 11 + ant собирают ядро и датапак из закреплённого коммита официального репозитория `gitlab.com/Tryskell/acis_public`, в рантайм-слой попадают только JRE 11 и результат сборки. Один образ, три роли: `init-db`, `login`, `game`.
- **Конфиги** — стоковые из aCis; `acis/entrypoint.sh` при старте подменяет только нужные ключи (JDBC, логины БД, адреса). Отсутствующий ключ — ошибка, а не тихий пропуск.
- **Клиентский поток:** клиент → login:2106 (авторизация, список серверов) → login отдаёт адрес game из `GAME_PUBLIC_HOST` (127.0.0.1) → клиент → game:7777.
- **hexid:** game-сервер без `hexid.txt` не стартует. Entrypoint один раз генерирует его в томе `game-state`, login принимает новый сервер (`AcceptNewGameServer = True`) и записывает в `l2_login.gameservers`.

| Что | Где | Удаление |
|---|---|---|
| БД | том `db-data` | `docker compose down -v` |
| hexid game-сервера | том `game-state` | `docker compose down -v` |
| Геодата | `./geodata/` | удалить папку |
| Клиент Interlude (746, пропатченный system) | `./client/`, не в git | удалить папку |
| Логи | `./logs/login`, `./logs/game` | удалить папку |

**Память (лимиты контейнеров):** db 768 МБ (buffer pool 256 МБ), login 256 МБ, game 4 ГБ (heap 3 ГБ, из них около 1 ГБ занимает геодата). Итого до ~5 ГБ из 6–8 ГБ виртуалки; в простое фактически ~0,8 ГБ.

## Запуск

Требуется OrbStack (или Docker Desktop). Из корня репозитория:

```sh
docker compose up -d --build     # первая сборка ~2–5 минут
docker compose logs -f game      # ждать строку "Registered as server: [1] Bartz."
python3 scripts/smoke_test.py    # проверка протоколов login и game (746)
```

Клиент лежит в `./client/` (в git не попадает), winemac запускает его оттуда. Затем в клиенте через winemac: логин 127.0.0.1, любой логин/пароль (аккаунт создаётся автоматически при первом входе), сервер Bartz, создать персонажа.

Сделать персонажа GM (после создания, персонаж должен быть оффлайн):

```sh
docker compose exec db mariadb -ul2_game -pl2ai-game l2_game \
  -e "UPDATE characters SET accesslevel = 8 WHERE char_name = 'ИмяПерсонажа';"
```

Остановить: `docker compose down`. Полный сброс: `docker compose down -v && rm -rf logs`.

## LLM для ботов (по желанию)

Боты отвечают на свободный разговор (ПМ, пати-чат, общий чат по имени) через языковую модель. Ключ в git не попадает:

```sh
cp llm.env.example llm.env       # llm.env в .gitignore
open -e llm.env                  # вписать OPENAI_API_KEY=sk-... и сохранить
docker compose up -d --build     # пересобрать с новым кодом; потом при смене ключа хватит docker compose up -d game
docker compose logs game | grep "Autobots llm"   # "on: openai, model ..."
```

Без `llm.env` модель выключена, боты ведут себя как раньше. Провайдер, модель и лимиты запросов — `llm*` в
`data/autobots/config.xml` (подробно в `acis/autobots/README.md`); локальная Ollama — `llmProvider` = `ollama`.

## Геодата

aCis 382 загружает только формат `.l2d` (`XX_YY.l2d`), форматы L2J (`.l2j`) и L2OFF (`_conv.dat`) нужно конвертировать штатным `GeoDataConverter`. Геодата в git не хранится и лежит в `./geodata/`; без неё сервер стартует (entrypoint отключает список регионов), но без коллизий и линии видимости.

Текущий набор: 139 регионов из `.l2j` репозитория [ChaosPaladin/l2j-geodata-project](https://github.com/ChaosPaladin/l2j-geodata-project) (коммит `97332edd`, экспорт старого L2J geodata project, эпоха Gracia/High Five: в отдельных местах Interlude возможны неточности), сконвертированы в `.l2d`:

```sh
docker run --rm --entrypoint sh -v "$PWD/path/to/l2j:/geo" l2aiserver/acis:382 -c \
  "cd /opt/acis/gameserver && sed -i 's#^GeoDataPath.*#GeoDataPath = /geo/#' config/geoengine.properties \
   && echo J | java -Xmx1g -cp './libs/*' net.sf.l2j.geodataconverter.GeoDataConverter"
```

Затем `*.l2d` скопировать в `./geodata/` и перезапустить `game`.

## Смена ревизии aCis

Ревизия, коммит и JDK задаются вместе в `.env` (там же список проверенных коммитов). После смены: `docker compose down -v && docker compose up -d --build`.
