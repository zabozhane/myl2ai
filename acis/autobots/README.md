# L2Autobots on aCis 382 (work in progress)

Source: https://github.com/Elfocrash/L2Autobots, commit f990c61 (Kotlin, written for aCis 382).

## Files
- `acis-382-autobots.patch` - upstream `patch.diff` rebased onto stock aCis 382 (commit 452cd109):
  paths mapped from `src/main/java` to `aCis_gameserver/java`, applies to sources with LF line endings
  (aCis ships CRLF). Upstream diff was made against an already partly patched tree, so three hunks were
  redone by hand: IdFactory (items cleanup also keeps autobot items), Player (import), Say2 (onChat hook).
- `build.gradle.kts` - compiles only the Kotlin module against aCis Java sources (kotlinc reads them for
  symbols), producing a jar plus runtime deps that go into `aCis_gameserver/lib` before the stock ant build.

## Status / next steps
1. Get the Kotlin module to compile (blocked so far by Maven Central HTTP 429 in the cloud build).
2. Check what the patch lacks: AdminAutobots handler registration, AutobotsManager start-up, `sql/*.sql`
   and `data/autobots/*.xml` deployment.
3. Wire into `acis/Dockerfile`; geodata: aCis 382 loads only `.l2d` (L2OFF/L2J need GeoDataConverter).
