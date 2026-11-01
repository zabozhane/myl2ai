#!/usr/bin/env bash
# Build through this wrapper so the image and startup log identify the checked-out source.
set -euo pipefail
cd "$(dirname "$0")/.."
export BUILD_REVISION
BUILD_REVISION="$(git rev-parse HEAD)"
export BUILD_DIRTY=false
if [ -n "$(git status --porcelain --untracked-files=all -- acis docker-compose.yml scripts/build.sh)" ]; then
  BUILD_DIRTY=true
fi
printf 'Building revision=%s runtimeChanges=%s\n' "$BUILD_REVISION" "$BUILD_DIRTY"
docker compose build "$@"
