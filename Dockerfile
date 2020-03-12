# STAGE 1 - BUILD
FROM harbor.v2.dcas.dev/library/gradle:jdk13
LABEL maintainer="Django Cass <django@dcas.dev>"

WORKDIR /app

# Dry run for caching
COPY . .

RUN gradle build -x test

# STAGE 2 - RUN
FROM harbor.v2.dcas.dev/djcass44/adoptopenjdk-spring-base:13-alpine-jre
LABEL maintainer="Django Cass <django@dcas.dev>"

ENV USER=jmp

RUN addgroup -S ${USER} && adduser -S ${USER} -G ${USER}

WORKDIR /app
COPY --from=0 /app/build/libs/jmp.jar .

EXPOSE 7000

RUN chown -R ${USER}:${USER} /app
USER jmp

CMD ["java", "-jar", "jmp.jar"]
