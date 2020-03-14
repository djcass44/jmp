# STAGE 1 - BUILD
FROM harbor.v2.dcas.dev/library/base/gradle
LABEL maintainer="Django Cass <django@dcas.dev>"

WORKDIR /app

# Dry run for caching
COPY . .

RUN gradle build -x test

# STAGE 2 - RUN
FROM harbor.v2.dcas.dev/library/base/tomcat-native:master
LABEL maintainer="Django Cass <django@dcas.dev>"

WORKDIR /app
COPY --from=0 /app/build/libs/jmp.jar .

EXPOSE 7000

RUN chown -R somebody:0 /app
USER somebody

CMD ["java", "-jar", "jmp.jar"]
