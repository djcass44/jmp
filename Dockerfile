# STAGE 1 - BUILD
FROM adoptopenjdk/openjdk12:jdk-12.0.1_12-alpine-slim as GRADLE_CACHE

WORKDIR /app

# Dry run for caching
COPY build.gradle.kts settings.gradle version.xml gradlew ./
COPY gradle ./gradle
RUN ./gradlew build || return 0
# Build & package app
COPY . .

RUN ./gradlew buildPackage

# STAGE 2 - RUN
FROM adoptopenjdk/openjdk12:jre-12.0.1_12-alpine
LABEL maintainer="Django Cass <dj.cass44@gmail.com>"

ENV DRIVER_URL="jdbc:sqlite:jmp.db" \
    DRIVER_CLASS="org.sqlite.JDBC" \
    DRIVER_USER="" \
    DRIVER_PASSWORD="" \
    LOG_DIRECTORY="." \
    BASE_URL="localhost:8080" \
    JMP_HOME="/data/" \
    SOCKET_ENABLED=true \
    SOCKET_HOST=0.0.0.0 \
    SOCKET_PORT=7001

WORKDIR /app
COPY --from=GRADLE_CACHE /app/build/libs/jmp.jar .

EXPOSE 7000
VOLUME ["/data"]

COPY entrypoint.sh /entrypoint.sh

# Add Tini
ENV TINI_VERSION v0.18.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini

# Add wait-for-it.sh
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

ENTRYPOINT ["/tini", "--"]
CMD ["/entrypoint.sh"]