# STAGE 1 - BUILD
FROM openjdk:11.0.1-slim-stretch as GRADLE_CACHE

WORKDIR /app

# Dry run for caching
COPY build.gradle.kts settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew build || return 0
# Build & package app
COPY . .

RUN ./gradlew shadowJar

# STAGE 2 - RUN
FROM openjdk:11.0.1-slim-stretch
LABEL maintainer="dj.cass44@gmail.com"

ENV DRIVER_URL="jdbc:sqlite:jmp.db"
ENV DRIVER_CLASS="org.sqlite.JDBC"
ENV ENV_LOG_REQUEST_DIRECTORY="."
ENV BASE_URL="localhost:8080"

WORKDIR /app
COPY --from=GRADLE_CACHE /app/build/libs/jmp.jar .

EXPOSE 7000
VOLUME ["/data"]

ENTRYPOINT ["java", "-jar", "jmp.jar"]
CMD ["using", "env"]