# STAGE 1 - BUILD
FROM gradle:6.0.1-jdk13 as GRADLE_CACHE
LABEL maintainer="Django Cass <django@dcas.dev>"

WORKDIR /app

# Dry run for caching
COPY . .

RUN gradle build -x test

# STAGE 2 - RUN
FROM djcass44/adoptopenjdk-spring-base:13-alpine-jre
LABEL maintainer="Django Cass <django@dcas.dev>"

ENV USER=jmp

RUN addgroup -S ${USER} && adduser -S ${USER} -G ${USER}

WORKDIR /app
COPY --from=GRADLE_CACHE /app/build/libs/jmp.jar .

EXPOSE 7000

RUN chown -R ${USER}:${USER} /app
USER jmp

CMD ["java", "-jar", "jmp.jar"]