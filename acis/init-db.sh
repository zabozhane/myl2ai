#!/bin/sh
# Creates aCis tables on first start. A schema that already has tables is left untouched,
# so player data survives restarts; a full reset is "docker compose down -v".
set -eu

SQL_DIR=/opt/acis/sql
ADDON_SQL_DIR=$SQL_DIR/addons
LOGIN_TABLE_FILES="accounts.sql gameservers.sql"

run_sql() {
	user=$1 password=$2 schema=$3
	shift 3
	MYSQL_PWD=$password mariadb --host=db --user="$user" "$@" "$schema"
}

table_count() {
	user=$1 password=$2 schema=$3
	echo "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$schema';" \
		| run_sql "$user" "$password" "$schema" --skip-column-names
}

is_login_table_file() {
	case " $LOGIN_TABLE_FILES " in
		*" $1 "*) return 0 ;;
		*) return 1 ;;
	esac
}

import_login_schema() {
	for file_name in $LOGIN_TABLE_FILES; do
		echo "init-db: login <- $file_name"
		run_sql "$LOGIN_DB_USER" "$LOGIN_DB_PASSWORD" "$LOGIN_DB_NAME" < "$SQL_DIR/$file_name"
	done
}

import_game_schema() {
	for file in "$SQL_DIR"/*.sql; do
		file_name=$(basename "$file")
		is_login_table_file "$file_name" && continue
		echo "init-db: game <- $file_name"
		run_sql "$GAME_DB_USER" "$GAME_DB_PASSWORD" "$GAME_DB_NAME" < "$file"
	done
}

import_if_empty() {
	label=$1 user=$2 password=$3 schema=$4 importer=$5
	existing_tables=$(table_count "$user" "$password" "$schema")
	if [ "$existing_tables" -gt 0 ]; then
		echo "init-db: $label schema '$schema' already has $existing_tables tables, skipping"
		return
	fi
	$importer
}

import_if_empty login "$LOGIN_DB_USER" "$LOGIN_DB_PASSWORD" "$LOGIN_DB_NAME" import_login_schema
import_if_empty game "$GAME_DB_USER" "$GAME_DB_PASSWORD" "$GAME_DB_NAME" import_game_schema
# Addon tables (L2Autobots) use CREATE TABLE IF NOT EXISTS, so they are applied on every start:
# this also adds them to a game schema created before the addon existed.
for file in "$ADDON_SQL_DIR"/*.sql; do
	echo "init-db: game addon <- $(basename "$file")"
	run_sql "$GAME_DB_USER" "$GAME_DB_PASSWORD" "$GAME_DB_NAME" < "$file"
done
echo "init-db: done"
