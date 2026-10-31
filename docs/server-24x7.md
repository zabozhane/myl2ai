# Выделенный сервер 24/7 на macOS (Intel) + CI/CD

Схема: Intel-мак стоит как домашний сервер, на нём крутится весь стек
(db + login + game) и self-hosted GitHub runner. Клиент запускается с другой
машины (M4) по LAN. Пуш в `main` → runner на сервере сам пересобирает образ,
перезапускает стек и откатывается, если smoke-тест не прошёл.

Расчёт памяти: стек занимает ~7 ГБ (db 768 МБ + login 256 МБ + game 6 ГБ),
на машине с 16 ГБ без клиента и браузеров остаётся комфортный запас.

## 1. Подготовка машины

```sh
# Репозиторий — строго в ~/l2aiserver (workflow ходит именно туда)
git clone https://github.com/zabozhane/myl2ai.git ~/l2aiserver
cd ~/l2aiserver
cp llm.env.example llm.env   # при необходимости вписать OPENAI_API_KEY
```

Создать `.env` (не в git) на основе `.env` с основной машины и поменять два ключа:

```ini
# .env на сервере
BIND_ADDR=0.0.0.0                 # слушать LAN, а не только localhost
GAME_PUBLIC_HOST=192.168.1.50     # LAN-адрес ЭТОГО мака (см. п. 2)
```

Поставить [OrbStack](https://orbstack.dev) (легче Docker Desktop по памяти),
в настройках: **Memory limit ≥ 8 GB**, включить **Start at login**.

Первый подъём — как обычно (`docker compose up -d --build`, smoke-тест),
плюс скопировать `geodata/` с основной машины, если она нужна:

```sh
rsync -a m4-host:Projects/l2aiserver/geodata/ ~/l2aiserver/geodata/
```

## 2. Сеть

1. В роутере привязать серверу постоянный IP (DHCP reservation по MAC) —
   тот самый `192.168.1.50` из `GAME_PUBLIC_HOST`.
2. Файрвол macOS: либо выключен (домашняя LAN), либо разрешить входящие
   для OrbStack — порты 2106 и 7777 пробрасывает он.
3. Клиент на M4: вместо `127.0.0.1` подключаться к `192.168.1.50`
   (логин-порт тот же, 2106; game-адрес login-сервер отдаст сам из
   `GAME_PUBLIC_HOST`).

## 3. Электропитание и сон (чтобы жил 24/7)

```sh
sudo pmset -c sleep 0          # не засыпать от розетки
sudo pmset -c displaysleep 15  # экран гасить можно
sudo pmset -a autorestart 1    # подняться после сбоя питания
sudo pmset -a womp 1           # wake on lan, пригодится
```

Включить **автоматический вход** пользователя (System Settings → Users &
Groups → Automatic login) — OrbStack и GitHub runner стартуют в сессии
пользователя, без логина они не поднимутся.

## 4. Self-hosted GitHub runner

В репозитории: **Settings → Actions → Runners → New self-hosted runner**,
macOS x64. Дальше на сервере (команды даст страница GitHub, токен свой):

```sh
mkdir ~/actions-runner && cd ~/actions-runner
# скачать и распаковать runner по инструкции со страницы, затем:
./config.sh --url https://github.com/zabozhane/myl2ai \
  --token <ТОКЕН> --name l2server --labels l2server --unattended
./svc.sh install   # запуск через launchd, переживает перезагрузку
./svc.sh start
```

Результат: пуш в `main` → workflow `.github/workflows/deploy.yml` →
`scripts/deploy.sh` на сервере: git reset до `origin/main`, бэкап БД,
пересборка образа, `up -d`, протокольный smoke-тест; при провале — откат
на предыдущий образ. Ход деплоя виден во вкладке Actions.

Важно: self-hosted runner безопасен только в **приватном** репозитории —
любой пуш выполняет код на сервере.

## 5. Бэкапы и сторожок (launchd)

`scripts/backup_db.sh` — дамп `l2_game` + `l2_login` в `backups/` (gzip,
хранит последние 14). `scripts/watchdog.sh` — smoke-тест, при двойном
провале перезапускает login+game (падения контейнеров и так лечит
`restart: unless-stopped`, это страховка от «контейнер жив, но молчит»).

Плейсы кладутся в `~/Library/LaunchAgents/`:

`~/Library/LaunchAgents/com.l2aiserver.backup.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>com.l2aiserver.backup</string>
  <key>ProgramArguments</key>
  <array><string>/Users/ВАШ_ЮЗЕР/l2aiserver/scripts/backup_db.sh</string></array>
  <key>StartCalendarInterval</key>
  <dict><key>Hour</key><integer>5</integer><key>Minute</key><integer>17</integer></dict>
  <key>StandardOutPath</key><string>/Users/ВАШ_ЮЗЕР/l2aiserver/logs/backup.log</string>
  <key>StandardErrorPath</key><string>/Users/ВАШ_ЮЗЕР/l2aiserver/logs/backup.log</string>
</dict>
</plist>
```

`~/Library/LaunchAgents/com.l2aiserver.watchdog.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>com.l2aiserver.watchdog</string>
  <key>ProgramArguments</key>
  <array><string>/Users/ВАШ_ЮЗЕР/l2aiserver/scripts/watchdog.sh</string></array>
  <key>StartInterval</key><integer>300</integer>
  <key>StandardOutPath</key><string>/Users/ВАШ_ЮЗЕР/l2aiserver/logs/watchdog.log</string>
  <key>StandardErrorPath</key><string>/Users/ВАШ_ЮЗЕР/l2aiserver/logs/watchdog.log</string>
</dict>
</plist>
```

Активировать (подставив своё имя пользователя в путях):

```sh
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/com.l2aiserver.backup.plist
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/com.l2aiserver.watchdog.plist
```

## 6. Эксплуатация

| Задача | Как |
|---|---|
| Деплой | `git push` в `main` с M4 — дальше само |
| Деплой вручную | `ssh сервер 'cd ~/l2aiserver && FORCE_DEPLOY=1 ./scripts/deploy.sh'` |
| Логи игры | `ssh сервер 'cd ~/l2aiserver && docker compose logs -f game'` |
| Откат кода | revert-коммит в `main` — пайплайн развернёт его как обычный деплой |
| Восстановить БД | `gunzip -c backups/auto-....sql.gz \| docker compose exec -T db mariadb -uroot -p<root-пароль>` |
| Обновить ревизию aCis | правка `.env` на сервере + `FORCE_DEPLOY=1 ./scripts/deploy.sh` |

Деплой оставляет мир недоступным на время пересборки game (на i5 —
несколько минут). Боты сохраняются в БД, так что «развитие» мира между
деплоями не теряется; откат образа БД не трогает.
