# STAGE 1 - BUILD
FROM openjdk:11.0.1-slim-stretch as GRADLE_CACHE

WORKDIR /app

# Dry run for caching
COPY build.gradle.kts settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew build || return 0
# Build & package app
COPY . .

# replace localhost with the actual application url in similar.js
RUN awk 'NR==FNR{rep=(NR>1?rep RS:"") $0; next} {gsub(/BASE_URL="http:\/\/localhost:7000"/,rep)}1' env src/main/resources/public/api/similar.js > src/main/resources/public/api/similar.js.tmp
RUN mv src/main/resources/public/api/similar.js.tmp src/main/resources/public/api/similar.js

# replace localhost with the actual application url in tokcheck.html
RUN awk 'NR==FNR{rep=(NR>1?rep RS:"") $0; next} {gsub(/BASE_URL="http:\/\/localhost:7000"/,rep)}1' env src/main/resources/public/api/tokcheck.html > src/main/resources/public/api/tokcheck.html.tmp
RUN mv src/main/resources/public/api/tokcheck.html.tmp src/main/resources/public/api/tokcheck.html


RUN ./gradlew shadowJar

# STAGE 2 - RUN
FROM openjdk:11.0.1-slim-stretch

LABEL maintainer="dj.cass44@gmail.com"

WORKDIR /app
COPY --from=GRADLE_CACHE /app/build/libs/jmp.jar .

EXPOSE 7000
VOLUME ["/data"]

ENTRYPOINT ["java", "-jar", "jmp.jar"]
CMD ["using", "config.json"]