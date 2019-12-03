# Configuration

## Database configuration

Database connection is established using values set in "Application configuration"

JMP currently supports the following databases:
* SQLite (default)
* PostgreSQL
* MySQL
* H2
* SQLServer

## Application configuration

`BASE_URL`: the url that the application will be running on (default `http://localhost:8080`)

`JMP_HOME`: the directory used to store data (default . (current directory))

`JMP_ALLOW_ERROR_INFO`: allow error information in the web ui (default `false`)

`JMP_ALLOW_EGRESS`: allow network requests leaving JMP. These are used for things such as scraping website metadata.
Note: this has no effect on requests made to authorisation servers (e.g. Crowd, OAuth2)

`KEY_REALM`: the source of the JWT encryption key (`java` or `aws-ssm`) (default `java`)

`KEY_AWS_SSM_NAME`: the AWS ParameterStore parameter name (required if KEY_REALM is `aws-ssm`)

`CASE_SENSITIVE`: jump case should be checked (default `false`)

`FAV2_URL`: the url of the [fav2 microservice](https://github.com/djcass44/fav2fav2 microservice) (optional)

## Authentication configuration

### OAuth2

JMP allows delegating user authentication to a 3rd party using the OAuth2 standard.
It currently supports GitHub & Google, however this will be expanding.

### JMP-managed

This includes the following authentication providers:

* Internal
* LDAP (OpenLDAP/Microsoft AD)
* Atlassian Crowd

LDAP configuration allows for user searching using a service account (recommended) or anonymously.
In order to do anonymous searches, you must set the `ldap.username` or `ldap.password` values to blank `""`

# Configuration

Application configuration is done via the `jmp.yaml` file located in `JMP_HOME`.
This file allows you to configure the application and authentication.

Fields can be overridden by environment/system properties. For example:

**Override server port**
```shell script
export JMP_SERVER_PORT=8080
java -jar jmp.jar
```

**Disable LDAP**
```shell script
java -jar jmp.jar -Dldap.enabled=false
```

**Enabled GitHub OAuth2**
```shell script
docker run -e OAUTH2_GITHUB_ENABLED=true djcass44/jmp
```

This file is automatically created on first-run however you can pre-create it.

```yaml
version: "2019-10-02" # schema version, don't change
blockLocal: false # block creating local user accounts
jmp:
  server:
    port: 7000 # port to run on
    ssl: false # enable/disable TLS
    keyStore: "" # path to java keystore
    keyStorePassword: "" # password for java keystore
    http2: false # enable/disable HTTP/2 (requires ssl -> true)
  database:
    url: "jdbc:sqlite:jmp.db" # JDBC url for database
    driverClass: "org.sqlite.JDBC" # JDBC class
    username: "" # database username (optional)
    password: "" # database password (optional)
crowd:
  enabled: false # enable/disable crowd integration
  url: "http://localhost:8095/crowd" # crowd url
  username: "user" # jmp app username
  password: "hunter2" # jmp app password
ldap:
  enabled: false # enable/disable ldap integration
  url: "localhost" # hostname of ldap server
  port: 389 # port of ldap server
  contextDN: "dc=example,dc=org" # base DN to search
  uidField: "uid" # field to identify user
  username: "user" # service account userDN
  password: "hunter2" # service account password
oauth2: # oauth2 providers
  github:
    enabled: true
    callbackUrl: https://jmp.example.org/callback-github
    clientId: 1234
    clientSecret: 1234
```