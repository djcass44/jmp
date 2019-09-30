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

Edit or create a file in JMP_HOME called jmp.properties

This file will allow you to configure LDAP/Atlassian Crowd

```properties
type=ldap or crowd
ldap=true or false
ldap.host=localhost
ldap.port=389
ldap.context=ldap serviceaccount dn
ldap.user=ldap serviceaccount user
ldap.password=ldap serviceaccount password
jmp.ldap.user_query=ldap query to get users
jmp.ldap.group_filter=ldap query to filter groups
jmp.ldap.group_query=ldap query to get groups
jmp.ldap.user_uid=ldap field for a unique user id
jmp.ldap.group_uid=ldap field for a unique group id
jmp.ldap.max_failure=5 # max number of attemps to connect to ldap
jmp.ldap.auth_reconnect=true # reconnect service account when a user logs in
jmp.ldap.remove_stale=true # remove jmp users no longer in ldap
jmp.ldap.sync_rate=300000 # number of milliseconds between each sync
jmp.ext.block_local=false # block local user creation
# crowd configuration
crowd=true or false
crowd.url=https://crowd.example.org
crowd.user=app username to connect to crowd
crowd.pass=app password to connect to crowd
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