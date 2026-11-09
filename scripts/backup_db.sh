#!/usr/bin/env bash
# Daily DB backup for the 24/7 server. Dumps l2_game + l2_login from the db
# container into ./backups, gzipped; keeps the newest KEEP_LAST dumps.
# Safe to run while the game is online (mariadb-dump --single-transaction).
#
# Usage: scripts/backup_db.sh   (from the repo root; also called by deploy.sh)
set -euo pipefail

cd "$(dirname "$0")/.."

KEEP_LAST="${KEEP_LAST:-14}"
STAMP="$(date +%Y-%m-%d_%H%M)"
OUT="backups/auto-${STAMP}.sql.gz"

mkdir -p backups

# Schema names come from .env (gitignored, per-machine); defaults match the repo.
env_val() { grep -E "^$1=" .env 2>/dev/null | tail -1 | cut -d= -f2-; }
GAME_DB="$(env_val GAME_DB_NAME)";  GAME_DB="${GAME_DB:-l2_game}"
LOGIN_DB="$(env_val LOGIN_DB_NAME)"; LOGIN_DB="${LOGIN_DB:-l2_login}"

docker compose exec -T db sh -c \
  'exec mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --single-transaction --routines --databases "$@"' \
  sh "$GAME_DB" "$LOGIN_DB" \
  | gzip > "$OUT"

# Rotate: remove all but the newest KEEP_LAST auto-*.sql.gz
ls -1t backups/auto-*.sql.gz 2>/dev/null | tail -n "+$((KEEP_LAST + 1))" | xargs rm -f 2>/dev/null || true

echo "backup ok: $OUT ($(du -h "$OUT" | cut -f1))"
