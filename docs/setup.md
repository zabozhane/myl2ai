# Как развернуть сервер у себя (macOS и Windows)

Пошаговая инструкция для нового человека: склонировать репозиторий, поднять сервер в Docker, положить геодату,
по желанию включить LLM-«голову» ботов и зайти клиентом Interlude. Устройство сервера описано в [README](../README.md),
журнал работ — в [PROGRESS.md](../PROGRESS.md).

## Что получится

Три контейнера из `docker-compose.yml` плюс разовый `db-init`:

| Сервис | Что это | Память (лимит контейнера) |
|---|---|---|
| `db` | MariaDB 11.4, схемы `l2_login` и `l2_game` (том `db-data`) | 768 МБ |
| `db-init` | при первом старте заливает SQL aCis и L2Autobots в пустые схемы и завершается | — |
| `login` | login-сервер aCis 382, порт `127.0.0.1:2106` | 256 МБ (heap 64 МБ) |
| `game` | game-сервер aCis 382 с ботами L2Autobots, порт `127.0.0.1:7777` | 6 ГБ (heap до 5 ГБ) |

Java, Ant, Gradle и исходники aCis ставить на компьютер не нужно: образ `l2aiserver/acis:382` сам собирается
из закреплённых коммитов (`.env`) и патчей из `acis/autobots/`. Порты открыты только на `127.0.0.1`, снаружи сервер не виден.

## Требования

- **Docker Desktop** (macOS или Windows) свежей версии: нужен Docker Compose 2.24 или новее (`docker compose version`),
  в `docker-compose.yml` используется необязательный `env_file`. На Маке подойдёт и OrbStack.
- **Память для Docker не меньше 7,5 ГБ, лучше 8 ГБ**: `game` 6 ГБ + `db` 0,75 ГБ + `login` 0,25 ГБ. Значит, в компьютере
  нужно от 16 ГБ RAM. При меньшем лимите `game` будет убит по памяти, когда наберётся онлайн ботов
  (население `populationSize` = 1110, в пике около 555 онлайн).
- **Диск:** около 5 ГБ под образы Docker, 1 ГБ под геодату и ещё около 2,5 ГБ временно, пока геодата конвертируется.
- **Git** и **Python 3** (для проверочного скрипта `scripts/smoke_test.py`, он без зависимостей).
- **Клиент Lineage 2 Interlude** (C6, протокол 746), пропатченный на адрес `127.0.0.1`. В git его нет, см. шаг 6.
- По желанию: ключ **OpenAI API** для LLM-«головы» ботов (шаг 5).

## 1. Подготовить Docker

### macOS

1. Установить [Docker Desktop](https://www.docker.com/products/docker-desktop/) (или OrbStack) и запустить его.
2. Docker Desktop → **Settings → Resources → Memory**: поставить 8 GB (минимум 7,5). **Apply & restart**.
   В OrbStack: Settings → System → Memory limit.
3. Проверить в Терминале:

   ```sh
   docker compose version
   docker info | grep -i "total memory"
   ```

### Windows (10/11)

1. Включить WSL 2: в PowerShell от администратора `wsl --install`, перезагрузиться.
2. Установить [Docker Desktop](https://www.docker.com/products/docker-desktop/) с бэкендом WSL 2 (по умолчанию) и запустить.
3. Память. С WSL 2 ползунка памяти в Docker Desktop нет, лимит задаёт сама WSL (по умолчанию половина RAM: на 16 ГБ
   это ровно 8 ГБ, хватает). Если RAM больше или меньше, создать файл `%UserProfile%\.wslconfig`:

   ```ini
   [wsl2]
   memory=8GB
   ```

   Затем в PowerShell `wsl --shutdown` и перезапустить Docker Desktop.
4. Проверить в PowerShell:

   ```powershell
   docker compose version
   docker info | Select-String "Total Memory"
   ```

## 2. Склонировать репозиторий

### macOS

```sh
mkdir -p ~/Projects && cd ~/Projects
git clone https://github.com/zabozhane/myl2ai.git l2aiserver
cd l2aiserver
```

Папка `~/Projects/l2aiserver` — та, к которой привязан лаунчер winemac (шаг 6), но подойдёт любая.

### Windows

```powershell
cd $HOME
git clone --config core.autocrlf=false https://github.com/zabozhane/myl2ai.git l2aiserver
cd l2aiserver
```

`core.autocrlf=false` — чтобы Git не превратил переводы строк в CRLF: скрипты `acis/*.sh` и `mariadb/*.sh` выполняются
внутри Linux-контейнеров и с CRLF падают (`/bin/sh^M: not found`), а патчи из `acis/autobots/` перестают применяться.
В репозитории для этого есть `.gitattributes` (всё в LF), флаг — подстраховка для старых версий Git.
Если репозиторий уже склонирован с CRLF: `git config core.autocrlf false`, затем `git rm --cached -r .` и `git reset --hard`.

Настройки сервера лежат в `.env` (ревизия aCis, пароли БД только для локальной сети Docker, `GAME_JAVA_OPTS`).
Менять его для запуска не нужно.

## 3. Собрать и запустить сервер

Команды одинаковые для Терминала macOS и PowerShell, запускать из корня репозитория:

```sh
docker compose up -d --build
```

Первая сборка занимает несколько минут: скачиваются исходники aCis (GitLab) и L2Autobots (GitHub), Gradle и
зависимости, применяются патчи, собираются ядро, датапак и модуль ботов. Потом стартуют `db`, `db-init`, `login`, `game`.

Дождаться готовности:

```sh
docker compose logs -f login    # строка "Loginserver ready", Ctrl+C чтобы выйти
docker compose logs -f game     # строка "Registered as server: [1] Bartz."
```

Проверить протоколы login и game:

```sh
python3 scripts/smoke_test.py   # на Windows: py scripts\smoke_test.py
```

Пока геодаты нет, в логе `game` будет `WARNING no geodata files in /opt/acis/geodata, starting without geodata`:
сервер работает, но без коллизий и линии видимости. Это нормально до шага 4.

При первом старте сервер сразу набирает население ботов до `populationSize` (1110), поэтому в мире они появляются
не мгновенно.

## 4. Геодата

Геодата в git не хранится (папка `geodata/` в `.gitignore`, исходники — в `geodata-download/`, тоже в `.gitignore`).
aCis 382 читает только формат `.l2d`, поэтому `.l2j` из репозитория
[ChaosPaladin/l2j-geodata-project](https://github.com/ChaosPaladin/l2j-geodata-project) (коммит `97332edd`,
139 регионов, эпоха Gracia/High Five — в отдельных местах Interlude возможны неточности) конвертируются
штатным `GeoDataConverter` из собранного образа. Образ уже должен быть собран (шаг 3).

### macOS

```sh
git clone https://github.com/ChaosPaladin/l2j-geodata-project.git geodata-download/repo
git -C geodata-download/repo checkout 97332edd
docker run --rm --entrypoint sh -v "$PWD/geodata-download/repo/geodata:/geo" l2aiserver/acis:382 -c \
  "cd /opt/acis/gameserver && sed -i 's#^GeoDataPath.*#GeoDataPath = /geo/#' config/geoengine.properties \
   && echo J | java -Xmx1g -cp './libs/*' net.sf.l2j.geodataconverter.GeoDataConverter"
cp geodata-download/repo/geodata/*.l2d geodata/
ls geodata/*.l2d | wc -l          # 139
docker compose restart game
```

### Windows (PowerShell)

```powershell
git clone https://github.com/ChaosPaladin/l2j-geodata-project.git geodata-download/repo
git -C geodata-download/repo checkout 97332edd
docker run --rm --entrypoint sh -v "${PWD}/geodata-download/repo/geodata:/geo" l2aiserver/acis:382 -c "cd /opt/acis/gameserver && sed -i 's#^GeoDataPath.*#GeoDataPath = /geo/#' config/geoengine.properties && echo J | java -Xmx1g -cp './libs/*' net.sf.l2j.geodataconverter.GeoDataConverter"
Copy-Item geodata-download\repo\geodata\*.l2d geodata\
(Get-ChildItem geodata\*.l2d).Count   # 139
docker compose restart game
```

Конвертер спрашивает формат исходников, `echo J` отвечает «L2J». После рестарта предупреждения про геодату в логе
`game` быть не должно. Папку `geodata-download/` после этого можно удалить.

Геодата занимает около 1 ГБ из heap game-сервера — это уже учтено в `-Xmx5g`.

## 5. LLM для ботов (по желанию)

Боты рядом с игроком думают и разговаривают через языковую модель (OpenAI по ключу), остальные — на коде.
Без ключа или при сбое всё работает как раньше. Ключ лежит в `llm.env`, который в `.gitignore`.

macOS:

```sh
cp llm.env.example llm.env
open -e llm.env                  # вписать OPENAI_API_KEY=sk-... и сохранить
docker compose up -d game
docker compose logs game | grep "Autobots llm"    # "on: openai, model ..."
```

Windows:

```powershell
Copy-Item llm.env.example llm.env
notepad llm.env                  # вписать OPENAI_API_KEY=sk-... и сохранить
docker compose up -d game
docker compose logs game | Select-String "Autobots llm"
```

Провайдер, модель и лимиты запросов — настройки `llm*` в `data/autobots/config.xml` (подробно в
[acis/autobots/README.md](../acis/autobots/README.md)). Для другого OpenAI-совместимого провайдера вместо
`OPENAI_API_KEY` используется `LLM_API_KEY` (см. `llm.env.example`).

## 6. Клиент и вход в игру

Клиент Interlude (C6, протокол 746) в репозиторий не входит. Он должен быть пропатчен на `127.0.0.1`: `ServerAddr`
в `system/l2.ini` и ключ шифрования системных файлов, иначе клиент пойдёт на чужой сервер. Готовую папку клиента
проще всего взять у владельца проекта; её место — `client/` в корне репозитория (папка в `.gitignore`), исполняемый
файл — `client/Lineage 2 client/data/system/L2.exe`.

### macOS: через winemac

Клиент для Windows, на Маке (Apple Silicon) он запускается через Wine лаунчером
[winemac](https://github.com/zabozhane/winemac) — отдельный репозиторий, подробности в его README.

```zsh
git clone https://github.com/zabozhane/winemac.git ~/winemac
~/winemac/bin/winemac setup                         # Rosetta 2, winetricks через Homebrew, закреплённая сборка Wine
~/winemac/bin/winemac create l2
~/winemac/bin/winemac deps l2                       # d3dx9 vcrun2005 vcrun2008
~/winemac/bin/winemac import l2 "$HOME/Projects/l2aiserver/client/Lineage 2 client" data/system/L2.exe
~/winemac/bin/winemac server attach l2 ~/Projects/l2aiserver
~/winemac/bin/winemac run l2
```

После `server attach` команда `run` сама поднимает сервер (`docker compose up -d`), ждёт `Loginserver ready` и
`Registered as server` и только потом запускает клиент. Есть и приложение с кнопками: `~/winemac/app/build-app.sh`,
затем `open ~/winemac/WineMac.app`.

### Windows: напрямую

Клиент родной, Wine не нужен. Запустить `client\Lineage 2 client\data\system\L2.exe` (или из любой другой папки, куда
положен клиент). Если клиент не стартует из-за DirectX или библиотек, поставить
[DirectX End-User Runtime](https://www.microsoft.com/en-us/download/details.aspx?id=35) и Visual C++ 2005 и 2008
Redistributable (x86) — то же, что winemac ставит в бутылку на Маке.

### Вход

Сервер должен быть поднят (шаг 3). В клиенте: любой логин и пароль (аккаунт создаётся при первом входе),
сервер **Bartz**, создать персонажа.

Сделать персонажа GM (персонаж должен быть оффлайн), одинаково на обеих системах:

```sh
docker compose exec db mariadb -ul2_game -pl2ai-game l2_game -e "UPDATE characters SET accesslevel = 8 WHERE char_name = 'ИмяПерсонажа';"
```

Панель управления ботами в игре — `//a b`.

## Повседневные команды

| Что | Команда |
|---|---|
| Запустить | `docker compose up -d` |
| Остановить (данные остаются) | `docker compose down` |
| Логи game-сервера | `docker compose logs -f game` |
| Пересобрать после `git pull` | `docker compose up -d --build` |
| Отчёт по экономике ботов | `scripts/economy-report.sh 1h` (bash: на Windows — Git Bash или WSL) |
| Полный сброс (БД и hexid) | `docker compose down -v`, затем удалить папку `logs` |

## Если что-то не так

- **`game` перезапускается или пропадает по `OOMKilled`** (`docker inspect l2aiserver-game-1 --format '{{.State.OOMKilled}}'`):
  Docker дали меньше 7,5 ГБ. Поднять лимит (шаг 1). Уменьшать `GAME_JAVA_OPTS` в `.env` и `mem_limit` в
  `docker-compose.yml` можно, но тогда и `populationSize` — он задаётся в `acis/autobots/l2autobots-fixes.patch`
  и запекается в образ, после правки нужна пересборка.
- **`entrypoint.sh: not found` или `/bin/sh^M`** (Windows): скрипты получили CRLF. См. шаг 2, затем
  `docker compose build --no-cache`.
- **Сборка падает на `git fetch` с GitLab или GitHub**: временные лимиты или сеть. Повторить `docker compose up -d --build`.
- **Клиент висит на входе**: защита aCis от флуда банит адрес на 5 минут после частых подключений к 2106
  (через Docker все приходят с одного адреса). Подождать 5 минут и не опрашивать порт в цикле; `smoke_test.py`
  подключается один раз и не мешает.
- **Windows: порт 2106 или 7777 занят** (`ports are not available`): Hyper-V резервирует диапазоны портов.
  Посмотреть `netsh interface ipv4 show excludedportrange protocol=tcp`; обычно помогает `net stop winnat`,
  `docker compose up -d`, `net start winnat` (PowerShell от администратора).
- **Без геодаты** мобы и боты ходят сквозь стены — это ожидаемо, см. шаг 4.
