# STAGE 1 - BUILD
FROM gradle:5.6.4-jdk12 as GRADLE_CACHE
LABEL maintainer="Django Cass <dj.cass44@gmail.com>"

WORKDIR /app

# Dry run for caching
COPY . .

RUN gradle buildPackage -x test -x jacocoTestReport

# STAGE 2 - RUN
FROM adoptopenjdk/openjdk12:alpine-jre
LABEL maintainer="Django Cass <dj.cass44@gmail.com>"

ENV DRIVER_URL="jdbc:sqlite:jmp.db" \
    DRIVER_CLASS="org.sqlite.JDBC" \
    DRIVER_USER="" \
    DRIVER_PASSWORD="" \
    LOG_DIRECTORY="." \
    BASE_URL="localhost:8080" \
    JMP_HOME="/data/" \
    JMP_ALLOW_EGRESS=true \
    USER=jmp

RUN addgroup -S ${USER} && adduser -S ${USER} -G ${USER}

WORKDIR /app
COPY --from=GRADLE_CACHE /app/build/libs/jmp.jar .

EXPOSE 7000

RUN chown -R ${USER}:${USER} /app
USER jmp

ENTRYPOINT ["java", "-jar jmp.jar"]