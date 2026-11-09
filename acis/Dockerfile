# syntax=docker/dockerfile:1

# JDK must match the aCis revision: 381-406 -> 11, >406 -> 21.
ARG JDK_VERSION=11

FROM eclipse-temurin:${JDK_VERSION}-jdk AS source
ARG ACIS_COMMIT
ARG AUTOBOTS_COMMIT
RUN apt-get update \
 && apt-get install -y --no-install-recommends ant git ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /src
# Pinned commit of the official repository; the SHA identifies the exact source tree.
# A shallow git fetch is used because GitLab rate-limits anonymous archive downloads.
RUN test -n "${ACIS_COMMIT}" \
 && git init --quiet \
 && git fetch --quiet --depth 1 https://gitlab.com/Tryskell/acis_public.git "${ACIS_COMMIT}" \
 && git checkout --quiet FETCH_HEAD
RUN test -n "${AUTOBOTS_COMMIT}" \
 && git init --quiet /autobots \
 && git -C /autobots fetch --quiet --depth 1 https://github.com/Elfocrash/L2Autobots.git "${AUTOBOTS_COMMIT}" \
 && git -C /autobots checkout --quiet FETCH_HEAD
COPY autobots/*.patch /patches/
# aCis ships CRLF sources; the patch is kept with LF, so only the files it touches are normalized.
RUN cat /patches/acis-382-*.patch | grep '^+++ b/' | cut -c7- | sort -u | xargs sed -i 's/\r$//' \
 && git apply /patches/acis-382-autobots.patch \
 && git apply /patches/acis-382-trade.patch \
 && git apply /patches/acis-382-offlinefarm.patch \
 && git apply /patches/acis-382-loginflag.patch \
 && git apply /patches/acis-382-town-landing.patch \
 && git apply /patches/acis-382-offline-death-context.patch \
 && git apply /patches/acis-382-owner-action.patch \
 && git apply /patches/acis-382-derby-off.patch \
 && git apply /patches/acis-382-clan-invite.patch \
 && git -C /autobots apply /patches/l2autobots-fixes.patch \
 && git -C /autobots apply /patches/l2autobots-autofarm.patch \
 && git -C /autobots apply /patches/l2autobots-craft.patch \
 && git -C /autobots apply /patches/l2autobots-quests.patch \
 && git -C /autobots apply /patches/l2autobots-skills.patch \
 && git -C /autobots apply /patches/l2autobots-sessions.patch \
 && git -C /autobots apply /patches/l2autobots-partysize.patch \
 && git -C /autobots apply /patches/l2autobots-raids.patch \
 && git -C /autobots apply /patches/l2autobots-trade.patch \
 && git -C /autobots apply /patches/l2autobots-llm.patch \
 && git -C /autobots apply /patches/l2autobots-newbies.patch \
 && git -C /autobots apply /patches/l2autobots-drowning.patch \
 && git -C /autobots apply /patches/l2autobots-danger.patch \
 && git -C /autobots apply /patches/l2autobots-farspots.patch \
 && git -C /autobots apply /patches/l2autobots-gkfail.patch \
 && git -C /autobots apply /patches/l2autobots-potions.patch \
 && git -C /autobots apply /patches/l2autobots-gear.patch \
 && git -C /autobots apply /patches/l2autobots-fallback.patch \
 && git -C /autobots apply /patches/l2autobots-ledges.patch \
     && git -C /autobots apply /patches/l2autobots-deathlog.patch \
 && git -C /autobots apply /patches/l2autobots-offlinefarm.patch \
 && git -C /autobots apply /patches/l2autobots-repeatquests.patch \
     && git -C /autobots apply /patches/l2autobots-clans.patch \
 && git -C /autobots apply /patches/l2autobots-offlinejournal.patch \
 && git -C /autobots apply /patches/l2autobots-offlinechores.patch \
 && git -C /autobots apply /patches/l2autobots-quietfarm.patch \
 && git -C /autobots apply /patches/l2autobots-audit-life.patch \
 && git -C /autobots apply /patches/l2autobots-audit-chores.patch \
 && git -C /autobots apply /patches/l2autobots-audit-market.patch \
 && git -C /autobots apply /patches/l2autobots-offline-social.patch \
 && git -C /autobots apply /patches/l2autobots-hunt-pace.patch \
 && git -C /autobots apply /patches/l2autobots-movement-retry.patch \
 && git -C /autobots apply /patches/l2autobots-movement-paths.patch \
 && git -C /autobots apply /patches/l2autobots-gatekeeper-routes.patch \
 && git -C /autobots apply /patches/l2autobots-movement-status.patch \
 && git -C /autobots apply /patches/l2autobots-hunt-search.patch \
 && git -C /autobots apply /patches/l2autobots-town-policy.patch \
 && git -C /autobots apply /patches/l2autobots-travel-budget.patch \
 && git -C /autobots apply /patches/l2autobots-narrow-routes.patch \
 && git -C /autobots apply /patches/l2autobots-stranded-walk.patch \
 && git -C /autobots apply /patches/l2autobots-hunt-retry-progress.patch \
 && git -C /autobots apply /patches/l2autobots-hunt-journey.patch \
 && git -C /autobots apply /patches/l2autobots-timer-errors.patch \
 && git -C /autobots apply /patches/l2autobots-offline-planning-behavior.patch \
 && git -C /autobots apply /patches/l2autobots-town-walk.patch \
 && git -C /autobots apply /patches/l2autobots-supply-budget.patch \
 && git -C /autobots apply /patches/l2autobots-town-unstuck.patch \
 && git -C /autobots apply /patches/l2autobots-npc-return-paths.patch \
 && git -C /autobots apply /patches/l2autobots-unstuck-preparation.patch \
 && git -C /autobots apply /patches/l2autobots-thinking-recovery.patch \
 && git -C /autobots apply /patches/l2autobots-offline-travel-intent.patch \
 && git -C /autobots apply /patches/l2autobots-update-dispatch.patch \
 && git -C /autobots apply /patches/l2autobots-recovery-narrow-exits.patch \
 && git -C /autobots apply /patches/l2autobots-scarce-healing-cycle.patch \
 && git -C /autobots apply /patches/l2autobots-novice-target-window.patch \
 && git -C /autobots apply /patches/l2autobots-field-hunt-recovery.patch \
 && git -C /autobots apply /patches/l2autobots-quest-town-destination.patch \
 && git -C /autobots apply /patches/l2autobots-invalid-field-origin.patch \
 && git -C /autobots apply /patches/l2autobots-hunt-candidate-progress.patch \
 && git -C /autobots apply /patches/l2autobots-dry-origin-recovery.patch \
 && git -C /autobots apply /patches/l2autobots-elongated-origin-recovery.patch \
 && git -C /autobots apply /patches/l2autobots-moving-party-target.patch \
 && git -C /autobots apply /patches/l2autobots-offline-weapon-purchase.patch \
 && git -C /autobots apply /patches/l2autobots-funded-novice-weapon-trip.patch \
 && git -C /autobots apply /patches/l2autobots-weapon-grade-supplies.patch \
 && git -C /autobots apply /patches/l2autobots-stocked-inventory-bows.patch \
 && git -C /autobots apply /patches/l2autobots-empty-bow-fallback.patch \
 && git -C /autobots apply /patches/l2autobots-offline-profession-defense.patch \
 && git -C /autobots apply /patches/l2autobots-profession-retry-memory.patch \
 && git -C /autobots apply /patches/l2autobots-quest-home-gatekeeper.patch \
 && git -C /autobots apply /patches/l2autobots-empty-arrow-restock.patch \
 && git -C /autobots apply /patches/l2autobots-novice-travel-topup.patch \
 && git -C /autobots apply /patches/l2autobots-complete-quest-route.patch \
 && git -C /autobots apply /patches/l2autobots-fallback-target-window.patch \
 && git -C /autobots apply /patches/l2autobots-offline-death-context.patch \
 && git -C /autobots apply /patches/l2autobots-base-class-travel-budget.patch \
 && git -C /autobots apply /patches/l2autobots-persistent-hunt-fitness.patch \
 && git -C /autobots apply /patches/l2autobots-town-hunt-funding.patch \
 && git -C /autobots apply /patches/l2autobots-novice-weapon-readiness.patch \
 && git -C /autobots apply /patches/l2autobots-cave-exit-search.patch \
 && git -C /autobots apply /patches/l2autobots-supply-walk-defense.patch \
 && git -C /autobots apply /patches/l2autobots-offline-deadly-spots.patch \
 && git -C /autobots apply /patches/l2autobots-supply-scroll-retry.patch \
 && git -C /autobots apply /patches/l2autobots-deadly-spot-store.patch \
 && git -C /autobots apply /patches/l2autobots-aggressive-crowd.patch \
 && git -C /autobots apply /patches/l2autobots-population-aggressive-crowd.patch \
 && git -C /autobots apply /patches/l2autobots-solo-killer-memory.patch \
 && git -C /autobots apply /patches/l2autobots-offline-armor-plan.patch \
 && git -C /autobots apply /patches/l2autobots-armor-plan-gain.patch \
 && git -C /autobots apply /patches/l2autobots-armor-kind.patch \
 && git -C /autobots apply /patches/l2autobots-death-penalty-recovery.patch \
 && git -C /autobots apply /patches/l2autobots-knight-skills.patch \
 && git -C /autobots apply /patches/l2autobots-guide-gear.patch \
 && git -C /autobots apply /patches/l2autobots-guide-skills.patch \
 && git -C /autobots apply /patches/l2autobots-offline-spellbooks.patch \
 && git -C /autobots apply /patches/l2autobots-population-attack-books.patch \
 && git -C /autobots apply /patches/l2autobots-fists-mystic-prey.patch \
 && git -C /autobots apply /patches/l2autobots-fists-mystic-groups.patch \
 && git -C /autobots apply /patches/l2autobots-population-killer-memory.patch \
 && git -C /autobots apply /patches/l2autobots-party-follow-progress.patch \
 && git -C /autobots apply /patches/l2autobots-town-walk-short-failure.patch \
 && git -C /autobots apply /patches/l2autobots-town-point-arrival.patch \
 && git -C /autobots apply /patches/l2autobots-offline-guide-armor.patch \
 && git -C /autobots apply /patches/l2autobots-guide-base-gear.patch \
 && git -C /autobots apply /patches/l2autobots-profession-travel-reserve.patch \
 && git -C /autobots apply /patches/l2autobots-guide-zones.patch \
 && git -C /autobots apply /patches/l2autobots-offline-guide-trip.patch \
 && git -C /autobots apply /patches/l2autobots-guide-zones-ride.patch \
 && git -C /autobots apply /patches/l2autobots-guide-zones-safety.patch \
 && git -C /autobots apply /patches/l2autobots-mandatory-offline-life.patch \
 && git -C /autobots apply /patches/l2autobots-guide-zones-aggro.patch \
 && git -C /autobots apply /patches/l2autobots-outgrown-anchor.patch \
 && git -C /autobots apply /patches/l2autobots-novice-near-aggro.patch \
 && git -C /autobots apply /patches/l2autobots-road-defense-resume.patch \
 && git -C /autobots apply /patches/l2autobots-guide-zones-20plus.patch \
 && git -C /autobots apply /patches/l2autobots-second-class.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-elves.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-knights.patch \
 && git -C /autobots apply /patches/l2autobots-quest-travel-retry.patch \
 && git -C /autobots apply /patches/l2autobots-quest-long-trip.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-orcs.patch \
 && git -C /autobots apply /patches/l2autobots-quest-town-hops.patch \
 && git -C /autobots apply /patches/l2autobots-quest-hop-walk.patch \
 && git -C /autobots apply /patches/l2autobots-quest-field-hop.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-rogues.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-mages.patch \
 && git -C /autobots apply /patches/l2autobots-auto-life.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-priests.patch \
 && git -C /autobots apply /patches/l2autobots-healing-potions-30.patch \
 && git -C /autobots apply /patches/l2autobots-quest-safe-spots.patch \
 && git -C /autobots apply /patches/l2autobots-profession-goal.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-warcryer.patch \
 && git -C /autobots apply /patches/l2autobots-autolife-restore.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-overlord.patch \
 && git -C /autobots apply /patches/l2autobots-caster-loot.patch \
 && git -C /autobots apply /patches/l2autobots-client-inventory.patch \
 && git -C /autobots apply /patches/l2autobots-quest-field-landing.patch \
 && git -C /autobots apply /patches/l2autobots-quest-npc-walk.patch \
 && git -C /autobots apply /patches/l2autobots-quest-oneway-walk.patch \
 && git -C /autobots apply /patches/l2autobots-quest-closeup-walk.patch \
 && git -C /autobots apply /patches/l2autobots-quest-martankus-valley.patch \
 && git -C /autobots apply /patches/l2autobots-unreachable-attackers.patch \
 && git -C /autobots apply /patches/l2autobots-walk-stall-log.patch \
 && git -C /autobots apply /patches/l2autobots-quest-resume-route.patch \
 && git -C /autobots apply /patches/l2autobots-quest-fares-grey-attackers.patch \
 && git -C /autobots apply /patches/l2autobots-derby.patch \
 && git -C /autobots apply /patches/l2autobots-novice-crowd-prey-memory.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-reformer.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-summoner.patch \
 && git -C /autobots apply /patches/l2autobots-second-class-dwarves.patch \
 && git -C /autobots apply /patches/l2autobots-grey-packs-quest-hops.patch \
 && git -C /autobots apply /patches/l2autobots-third-class-fallback.patch \
 && git -C /autobots apply /patches/l2autobots-quest-hunt-walks.patch \
 && git -C /autobots apply /patches/l2autobots-quest-fight-heals.patch \
 && git -C /autobots apply /patches/l2autobots-quest-evil-eye-basin.patch \
 && git -C /autobots apply /patches/l2autobots-quest-init-order.patch \
 && git -C /autobots apply /patches/l2autobots-clan-life.patch \
 && git -C /autobots apply /patches/l2autobots-respawn-watchdog.patch \
 && git -C /autobots apply /patches/l2autobots-gear-no-downgrade.patch

# The Kotlin module is compiled against the aCis Java sources and dropped into aCis_gameserver/lib,
# where the stock ant build picks it up for both compilation and the dist libs.
FROM gradle:8.14.3-jdk${JDK_VERSION} AS autobots
COPY --from=source /src /src
COPY --from=source /autobots /autobots
COPY autobots/build.gradle.kts autobots/settings.gradle.kts /build/
RUN cd /build && gradle --no-daemon --quiet installIntoAcisLib -PacisSources=/src -PautobotsSources=/autobots

FROM source AS build
COPY --from=autobots /src/aCis_gameserver/lib /src/aCis_gameserver/lib
RUN ant -noinput -buildfile aCis_gameserver/build.xml dist \
 && ant -noinput -buildfile aCis_datapack/build.xml build

FROM eclipse-temurin:${JDK_VERSION}-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends mariadb-client \
 && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/aCis_gameserver/build/dist/login      /opt/acis/login
COPY --from=build /src/aCis_gameserver/build/dist/gameserver /opt/acis/gameserver
COPY --from=build /src/aCis_datapack/build/gameserver/data   /opt/acis/gameserver/data
COPY --from=build /src/aCis_datapack/build/login/            /opt/acis/login/
COPY --from=build /src/aCis_datapack/build/sql               /opt/acis/sql
COPY --from=source /autobots/data/autobots /opt/acis/gameserver/data/autobots
COPY --from=source /autobots/sql           /opt/acis/sql/addons
COPY entrypoint.sh init-db.sh /opt/acis/bin/
RUN chmod +x /opt/acis/bin/*.sh
# Build identity is supplied by scripts/build.sh; direct compose builds remain explicitly unknown.
ARG BUILD_REVISION=unknown
ARG BUILD_DIRTY=unknown
ARG ACIS_COMMIT
ARG AUTOBOTS_COMMIT
LABEL org.opencontainers.image.revision="${BUILD_REVISION}"
RUN printf 'revision=%s\nworkingTreeDirty=%s\nacis=%s\nautobotsUpstream=%s\n' \
      "$BUILD_REVISION" "$BUILD_DIRTY" "$ACIS_COMMIT" "$AUTOBOTS_COMMIT" > /opt/acis/build-info.txt
ENTRYPOINT ["/opt/acis/bin/entrypoint.sh"]
