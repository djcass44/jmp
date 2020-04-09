# JMP

JMP is a utility used for quickly navigating to select websites/addresses.

It is made up of 2 main components; the UI and API. 
For setting up the UI and more info, see the UI [README](https://github.com/djcass44/jmp-react/blob/develop/README.md).

## Setting up the API

When the application first starts, an initial `admin` user is created. 
The password to this user is randomly generated and is printed to the console and written to the working directory in a file called `initialAdminPassword`


JMP can be installed in 2 different ways.

1. **Using docker** (recommended)

```bash
docker run -p 7000:7000 djcass44/jmp
```

There is also a docker-compose utility script you can use

```bash
docker-compose up -d
```

2. **Standalone**

This is done via running the Jar file manually.

```bash
./gradlew shadowJar
java -jar build/libs/jmp.jar
```


## Browser setup

This information is also available in a running UI instance via the `?` button or `/help`. It is more user-oriented.

*Note: replace $BASE_URL with the url the application will be running on.*

**Chrome**

1. Open settings (`chrome://settings`)
2. Manage search engines
3. Add
3.  a. Search engine = JMP
    b. Keyword = jmp
    c. URL = `$BASE_URL/jmp?query=%s`

**Firefox**

Skip setup using the [Firefox extension](https://addons.mozilla.org/en-US/firefox/addon/jmp-webext/)!

1. Open Bookmarks (`Ctrl + Shift + O`)
2. Add new
2.  - Name = JMP
    - Keyword = jmp
    - Location = `$BASE_URL/jmp?query=%s`

## Usage

*Prerequisites: you have set up a number of jump points.*

Queries are `jmp name`

This example is assuming you have g bound to `https://google.com`

Go to the address bar and type `jmp g` and press enter.

### License

JMP is released under the [Apache 2.0 license](LICENSE)
```
Copyright 2019 Django Cass

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### Contributing

I will happily accept contributions, just fork this repo and open a pull request!