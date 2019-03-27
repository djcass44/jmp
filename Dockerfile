# STAGE 1 - BUILD
FROM openjdk:11.0.1-slim-stretch as GRADLE_CACHE

WORKDIR /app

# Dry run for caching
COPY build.gradle.kts settings.gradle version.xml gradlew ./
COPY gradle ./gradle
RUN ./gradlew build || return 0
# Build & package app
COPY . .

RUN ./gradlew buildPackage

# STAGE 2 - RUN
FROM openjdk:11.0.1-slim-stretch
LABEL maintainer="dj.cass44@gmail.com"

ENV DRIVER_URL="jdbc:sqlite:jmp.db"
ENV DRIVER_CLASS="org.sqlite.JDBC"
ENV ENV_LOG_REQUEST_DIRECTORY="."
ENV BASE_URL="localhost:8080"
ENV DRIVER_USER=""
ENV DRIVER_PASSWORD=""
ENV JMP_HOME="/data/"

WORKDIR /app
COPY --from=GRADLE_CACHE /app/build/libs/jmp.jar .

EXPOSE 7000
VOLUME ["/data"]

COPY entrypoint.sh /entrypoint.sh

# Add Tini
ENV TINI_VERSION v0.18.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini

ENTRYPOINT ["/tini", "--"]
CMD ["/entrypoint.sh"]