#!/bin/sh
# Applies container settings to the stock aCis configs, then runs the requested role.
set -eu

ACIS_HOME=/opt/acis
GAME_SERVER_ID=1
DB_HOST=db
DB_PORT=3306

fail() {
	echo "entrypoint: $*" >&2
	exit 1
}

# Replaces "Key = value" in a properties file; a missing key is an error,
# because silently skipping it would leave a stock value such as localhost in place.
set_property() {
	file=$1 key=$2 value=$3
	grep -q "^${key}[[:space:]]*=" "$file" || fail "key '$key' not found in $file"
	escaped_value=$(printf '%s' "$value" | sed 's/[\\|&]/\\&/g')
	sed -i "s|^${key}[[:space:]]*=.*|${key} = ${escaped_value}|" "$file"
}

# aCis 382 ships MySQL Connector/J, aCis 409 ships MariaDB Connector/J.
jdbc_url() {
	schema=$1 libs_dir=$2
	if ls "$libs_dir"/mariadb-java-client-*.jar >/dev/null 2>&1; then
		echo "jdbc:mariadb://${DB_HOST}:${DB_PORT}/${schema}"
	else
		echo "jdbc:mysql://${DB_HOST}:${DB_PORT}/${schema}?serverTimezone=UTC"
	fi
}

configure_login() {
	config=$ACIS_HOME/login/config/loginserver.properties
	set_property "$config" URL "$(jdbc_url "$LOGIN_DB_NAME" "$ACIS_HOME/login/libs")"
	set_property "$config" Login "$LOGIN_DB_USER"
	set_property "$config" Password "$LOGIN_DB_PASSWORD"
	# Single private server: let the game server register itself instead of running RegisterGameServer by hand.
	set_property "$config" AcceptNewGameServer True
}

configure_game() {
	config_dir=$ACIS_HOME/gameserver/config
	set_property "$config_dir/server.properties" URL "$(jdbc_url "$GAME_DB_NAME" "$ACIS_HOME/gameserver/libs")"
	set_property "$config_dir/server.properties" Login "$GAME_DB_USER"
	set_property "$config_dir/server.properties" Password "$GAME_DB_PASSWORD"
	set_property "$config_dir/server.properties" LoginHost login
	# Address the login server hands to the client; the client runs on the Docker host.
	set_property "$config_dir/server.properties" Hostname "$GAME_PUBLIC_HOST"
	set_property "$config_dir/geoengine.properties" GeoDataPath /opt/acis/geodata/
	configure_geodata "$config_dir/geoengine.properties"
	ensure_hexid "$ACIS_HOME/gameserver/state/hexid.txt"
	ln -sf "$ACIS_HOME/gameserver/state/hexid.txt" "$config_dir/hexid.txt"
}

# The game server refuses to start without hexid.txt, which normally comes from the interactive
# RegisterGameServer tool. A random id is generated once instead; the login server accepts it
# (AcceptNewGameServer) and stores it. The file lives in a volume so the id survives container re-creation.
ensure_hexid() {
	hexid_file=$1
	[ -f "$hexid_file" ] && return
	hexid=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
	printf 'ServerID=%s\nHexID=%s\n' "$GAME_SERVER_ID" "$hexid" > "$hexid_file"
	echo "entrypoint: generated $hexid_file for server id $GAME_SERVER_ID"
}

# Without geodata files every listed region fails to load and the server exits,
# so with an empty geodata folder the region list is dropped (no collision checks in the world).
configure_geodata() {
	geoengine_config=$1
	if ls /opt/acis/geodata/ | grep -Eq '^[0-9]+_[0-9]+'; then
		return
	fi
	echo "entrypoint: WARNING no geodata files in /opt/acis/geodata, starting without geodata" >&2
	sed -i -E 's/^([0-9]+_[0-9]+)/#\1/' "$geoengine_config"
}

role=${1:-}
case "$role" in
	init-db)
		exec "$ACIS_HOME/bin/init-db.sh"
		;;
	login)
		configure_login
		cd "$ACIS_HOME/login"
		exec java $LOGIN_JAVA_OPTS -cp "./libs/*" net.sf.l2j.loginserver.LoginServer
		;;
	game)
		configure_game
		cd "$ACIS_HOME/gameserver"
		exec java $GAME_JAVA_OPTS -cp "./libs/*" net.sf.l2j.gameserver.GameServer
		;;
	*)
		fail "usage: entrypoint.sh init-db|login|game"
		;;
esac
