# Configuration

## Database configuration

Database connection is established using values set in "Application configuration"

JMP currently supports the following databases:
* SQLite
* PostgreSQL
* MySQL
* H2
* SQLServer

## Application configuration

`DRIVER_URL`: the JDBC url (default `jdbc:sqlite:jmp.db`)

`DRIVER_CLASS`: the JDBC class (default `org.sqlite.JDBC`)

`DRIVER_USER`: the username for database access (only required if the database requires it)

`DRIVER_PASSWORD`: the password for database access (only required if the database requires it)

`BASE_URL`: the url that the application will be running on (default `http://localhost:8080`)

`JMP_HTTP_SECURE`: enable SSL server configuration (default `false`)

`JMP_SSL_KEYSTORE`: path to jks keystore file (required if `JMP_HTTP_SECURE` is `true`)

`JMP_SSL_PASSWORD`: password for keystore (required if keystore has a password set)

`JMP_HTTP2`: enable HTTP/2 (default `true`). Requires `JMP_HTTP_SECURE` to be `true`

`JMP_HOME`: the directory used to store data (default . (current directory))

`JMP_ALLOW_ERROR_INFO`: allow error information in the web ui (default `false`)

`JMP_ALLOW_EGRESS`: allow network requests leaving JMP. These are used for things such as scraping website metadata.
Note: this has no effect on requests made to authorisation servers (e.g. Crowd, OAuth2)

`KEY_REALM`: the source of the JWT encryption key (`java` or `aws-ssm`) (default `java`)

`KEY_AWS_SSM_NAME`: the AWS ParameterStore parameter name (required if KEY_REALM is `aws-ssm`)

`CASE_SENSITIVE`: jump case should be checked (default `false`)

`PORT`: the port for the application to run on (default `8080`)

`LOG_ENABLED`: logs should be written to files (default `true`)

`FAV2_URL`: the url of the [fav2 microservice](https://github.com/djcass44/fav2fav2 microservice) (optional)

## Authentication configuration

### JMP-managed

#### jmp.json

`jmp.json` is a file located in `JMP_HOME` which allows for configuration of external identity providers. It currently supports LDAP and Atlassian Crowd.

This file is automatically created on first-run however you can pre-create it.

Commented example: (comments must be removed for actual use)

```yaml
{
  "version": "2019-10-02", // schema version, should never change
  "realm": "db", // set to ldap or crowd
  "min": {
    "enabled": false, // enable ldap/crowd
    "serviceAccount": { // service account for ldap/crowd
      "username": "username",
      "password": "password"
    },
    "syncRate": 300000, // number of ms to wait between syncing
    "maxConnectAttempts": 5, // number of failed connection attempts allowed
    "blockLocal": false, // disable database users when using ldap/crowd
    "removeStale": true // remove ldap/crowd users no longer found in ldap/crowd
  },
  "ldap": {
    "core": {
      "server": "localhost",
      "port": 389,
      "contextDN": "" // context of service account
    },
    "userFilter": "", // query to find users
    "uid": "", // field used as user id
    "reconnectOnAuth": false, // force reconnect to ldap
    "groups": {
      "groupFilter": "", // query to filter groups
      "groupQuery": "", // query to find groups
      "gid": "" // field used as group id
    }
  },
  "crowdUrl": "http://localhost:8095/crowd" // url used to reach crowd
}
```

### OAuth2

JMP allows delegating user authentication to a 3rd party using the OAuth2 standard.
It currently supports GitHub & Google, however this will be expanding.

**GitHub**

`GITHUB_ENABLED`: enable GitHub OAauth2

`GITHUB_CALLBACK`: the callback address set in your GitHub OAuth app (should be `https://jmp.example.org/callback-github`)

`GITHUB_CLIENT_ID`: the client ID provided by GitHub

`GITHUB_CLIENT_SECRET`: the client secret provided by GitHub

**Google**

`GOOGLE_ENABLED`: enable Google OAauth2

`GOOGLE_CALLBACK`: the callback address set in your Google consent app (should be `https://jmp.example.org/callback-google`)

`GOOGLE_CLIENT_ID`: the client ID provided by Google

`GOOGLE_CLIENT_SECRET`: the client secret provided by Google