FROM openjdk:11.0.1-slim-stretch

WORKDIR /app
COPY . /app

RUN ./gradlew shadowJar

EXPOSE 7000
VOLUME ["/data"]

ENTRYPOINT ["java", "-jar", "build/libs/jmp-1.0-SNAPSHOT.jar"]
CMD ["using", "/data/jmp.db"]