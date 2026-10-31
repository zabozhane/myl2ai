#!/usr/bin/env bash
# Deploy the latest origin/main on the 24/7 server. Called by the GitHub
# self-hosted runner (see .github/workflows/deploy.yml); can also be run by hand.
#
# Steps: fast-forward checkout -> DB backup -> rebuild -> up -> protocol smoke
# test -> automatic rollback to the previous image if the smoke test fails.
#
# Local, untracked state (geodata/, logs/, backups/, .env, llm.env, client/)
# survives the git reset --hard.
set -euo pipefail

cd "$(dirname "$0")/.."

BRANCH="${DEPLOY_BRANCH:-main}"
ROLLBACK_TAG=""

log() { echo "[deploy $(date +%H:%M:%S)] $*"; }

# --- 1. Update the checkout -------------------------------------------------
git fetch origin "$BRANCH"
if [ "$(git rev-parse HEAD)" = "$(git rev-parse "origin/$BRANCH")" ] && [ "${FORCE_DEPLOY:-0}" != "1" ]; then
  log "already at origin/$BRANCH ($(git rev-parse --short HEAD)), nothing to do"
  exit 0
fi
log "updating $(git rev-parse --short HEAD) -> $(git rev-parse --short "origin/$BRANCH")"
git reset --hard "origin/$BRANCH"

# --- 2. Pre-deploy DB backup -------------------------------------------------
if docker compose ps --status running -q db 2>/dev/null | grep -q .; then
  scripts/backup_db.sh || log "WARN: backup failed, deploying anyway"
else
  log "db container not running yet, skipping backup"
fi

# --- 3. Rebuild --------------------------------------------------------------
env_val() { grep -E "^$1=" .env 2>/dev/null | tail -1 | cut -d= -f2-; }
REV="$(env_val ACIS_REVISION)"; REV="${REV:-382}"
IMAGE="l2aiserver/acis:$REV"

if docker image inspect "$IMAGE" >/dev/null 2>&1; then
  ROLLBACK_TAG="$IMAGE.rollback"
  log "tagging current image as $ROLLBACK_TAG"
  docker tag "$IMAGE" "$ROLLBACK_TAG"
fi

log "building $IMAGE"
docker compose build

# --- 4. Apply ----------------------------------------------------------------
log "docker compose up -d"
docker compose up -d

# --- 5. Smoke test with rollback --------------------------------------------
smoke() {
  local tries=18 # ~4-5 мин: на i5 загрузка геодаты при старте game небыстрая
  for i in $(seq 1 $tries); do
    if python3 scripts/smoke_test.py 127.0.0.1 127.0.0.1; then
      return 0
    fi
    log "smoke attempt $i/$tries failed, waiting 10s"
    sleep 10
  done
  return 1
}

if smoke; then
  log "deploy ok: $(git rev-parse --short HEAD)"
else
  log "ERROR: smoke test failed after deploy"
  if [ -n "$ROLLBACK_TAG" ]; then
    log "rolling back image to $ROLLBACK_TAG"
    docker tag "$ROLLBACK_TAG" "$IMAGE"
    docker compose up -d
    if smoke; then
      log "rollback ok, server is back on the previous build"
    else
      log "ERROR: rollback did not pass the smoke test either, check: docker compose logs game"
    fi
  fi
  exit 1
fi
