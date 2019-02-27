FROM openjdk:11.0.1-slim-stretch

WORKDIR /app
COPY . /app

# Build front end
RUN apt update -yq && apt install curl gnupg -yq && curl -sL https://deb.nodesource.com/setup_8.x | bash && apt install nodejs -yq
RUN cd /app/src/main/resources/public && npm install && cd /app

# Build & package app
RUN ./gradlew shadowJar

EXPOSE 7000
VOLUME ["/data"]

ENTRYPOINT ["java", "-jar", "build/libs/jmp-1.0-SNAPSHOT.jar"]
CMD ["using", "config.json"]