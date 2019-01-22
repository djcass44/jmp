# JMP

JMP is a utility used for quickly navigating to select websites/addresses

## Install

JMP can be installed in 2 different ways.

1. **Standalone**

1.a. As a jar

```
./gradlew shadowJar
java -jar build/libs/jmp-1.0-SNAPSHOT.jar using env
```
1.b. Using gradle

```
./gradlew run
```

2. **Using docker** (recommended)

Edit env and add the url the application will be running on

```
docker build -t jmp:1 -f Dockerfile.prod .

docker run jmp:1 -v "./data:/data" -p 7000:7000
```

There is also a docker-compose utility script you can use

```
docker-compose up -d
```

## Setup

**Chrome**

1. Open settings (`chrome://settings`)

2. Manage search engines

3. Add

3.  a. Search engine = JMP
    b. Keyword = jmp
    c. URL = `$URL_IN_ENV/v1/jump/%s`

**Firefox**

1. Open Bookmarks (`Ctrl + Shift + O`)

2. Add new

2.  a. Name = JMP
    b. Keyword = jmp
    c. Location = `$URL_IN_ENV/v1/jump/%s`

## Usage

*Prerequisites: you have set up a number of jump points.*

Queries are `jmp name`

This example is assuming you have g bound to `https://google.com`

Go to the address bar and type `jmp g` and press enter.

## Contributing

PRs accepted.

## License

Apache License Version 2.0 Django Cass

See [license](LICENSE) for more information