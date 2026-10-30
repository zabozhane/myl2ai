#!/usr/bin/env bash
# Short report on the bots' economy from the game server output: the periodic summaries, a few deals of each
# kind, bot errors and the container's load. Usage: scripts/economy-report.sh [since]  (default 1h, e.g. 30m, 3h)
set -euo pipefail
cd "$(dirname "$0")/.."

since="${1:-1h}"
log="$(docker compose logs --no-color --since "$since" game 2>/dev/null)"

echo "== Summaries (every 10 min) =="
grep -o 'Autobots economy summary: .*' <<<"$log" | tail -n 6 || echo "none yet"

echo
echo "== Deals in the last $since =="
deals="$(grep -o 'Autobots economy: .*' <<<"$log" || true)"
for kind in "from NPC" "sold to NPC" "from store of" "to store of" "equipped" "enchanted" "failed" \
            "opened sell store" "opened buy store" "mode "; do
  matches="$(grep -F -- "$kind" <<<"$deals" || true)"
  count="$(grep -c . <<<"$matches" || true)"
  printf '%-18s %s\n' "$kind" "$count"
  [ "$count" -gt 0 ] && head -n 2 <<<"$matches" | sed 's/^Autobots economy: /    /'
done

echo
echo "== Bot errors =="
grep -B2 -A8 'dev\.l2j\.autobots' <<<"$log" | grep -E 'Exception|Error|at dev\.l2j' | sort | uniq -c | sort -rn | head -n 10 || echo "none"

echo
echo "== Load =="
docker stats --no-stream --format '{{.Name}}  CPU {{.CPUPerc}}  MEM {{.MemUsage}}' | grep -i game || true
