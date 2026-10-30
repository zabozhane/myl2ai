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
RUN grep '^+++ b/' /patches/acis-382-autobots.patch | cut -c7- | xargs sed -i 's/\r$//' \
 && git apply /patches/acis-382-autobots.patch \
 && git -C /autobots apply /patches/l2autobots-fixes.patch \
 && git -C /autobots apply /patches/l2autobots-autofarm.patch \
 && git -C /autobots apply /patches/l2autobots-craft.patch \
 && git -C /autobots apply /patches/l2autobots-quests.patch \
 && git -C /autobots apply /patches/l2autobots-skills.patch \
 && git -C /autobots apply /patches/l2autobots-sessions.patch

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
ENTRYPOINT ["/opt/acis/bin/entrypoint.sh"]
