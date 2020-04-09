# STAGE 1 - BUILD
FROM castive/gradle
LABEL maintainer="Django Cass <django@dcas.dev>"

USER root

WORKDIR /app

# Dry run for caching
COPY . .

RUN gradle build -x test

# STAGE 2 - RUN
FROM castive/tomcat-native
LABEL maintainer="Django Cass <django@dcas.dev>"

WORKDIR /app
COPY --from=0 /app/build/libs/jmp.jar .

EXPOSE 7000

RUN chown -R somebody:0 /app
USER somebody

CMD ["java", "-jar", "jmp.jar"]
