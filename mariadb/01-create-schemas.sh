#!/bin/sh
# Runs once, on the first start of an empty data volume (official MariaDB image hook).
# Login and game servers get separate schemas and users: neither server reads the other's tables.
set -eu

mariadb --user=root --password="$MARIADB_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS \`$LOGIN_DB_NAME\`;
CREATE DATABASE IF NOT EXISTS \`$GAME_DB_NAME\`;
CREATE USER IF NOT EXISTS '$LOGIN_DB_USER'@'%' IDENTIFIED BY '$LOGIN_DB_PASSWORD';
CREATE USER IF NOT EXISTS '$GAME_DB_USER'@'%' IDENTIFIED BY '$GAME_DB_PASSWORD';
GRANT ALL PRIVILEGES ON \`$LOGIN_DB_NAME\`.* TO '$LOGIN_DB_USER'@'%';
GRANT ALL PRIVILEGES ON \`$GAME_DB_NAME\`.* TO '$GAME_DB_USER'@'%';
SQL
