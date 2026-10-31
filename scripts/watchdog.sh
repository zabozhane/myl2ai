#!/usr/bin/env bash
# Watchdog for the 24/7 server, meant for launchd every few minutes
# (see docs/server-24x7.md). Container-level crashes are already covered by
# `restart: unless-stopped`; this catches a stack that is up but not answering
# the L2 protocol (hung game server) and restarts it.
#
# Quiet on success; prints to stdout (=> launchd log) only on action/failure.
set -uo pipefail

cd "$(dirname "$0")/.."

if python3 scripts/smoke_test.py 127.0.0.1 127.0.0.1 >/dev/null 2>&1; then
  exit 0
fi

# First failure might be a deploy in progress or a slow boot; recheck once.
sleep 60
if python3 scripts/smoke_test.py 127.0.0.1 127.0.0.1 >/dev/null 2>&1; then
  exit 0
fi

echo "[watchdog $(date '+%F %T')] smoke test failed twice, restarting game + login"
docker compose restart login game
