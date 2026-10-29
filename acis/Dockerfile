# syntax=docker/dockerfile:1

# JDK must match the aCis revision: 381-406 -> 11, >406 -> 21.
ARG JDK_VERSION=11

FROM eclipse-temurin:${JDK_VERSION}-jdk AS build
ARG ACIS_COMMIT
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
COPY entrypoint.sh init-db.sh /opt/acis/bin/
RUN chmod +x /opt/acis/bin/*.sh
ENTRYPOINT ["/opt/acis/bin/entrypoint.sh"]
