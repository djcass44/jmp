/*
 *    Copyright 2019 Django Cass
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.30"
    id("com.github.johnrengelman.shadow") version "4.0.3"
    application
}

group = "dev.castive"
version = "2.1"

apply(plugin = "java")

ant.importBuild("version.xml")

repositories {
    maven(url = "https://dl.bintray.com/kotlin/exposed")
    maven(url = "https://jitpack.io")
    jcenter()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")

//    implementation("com.github.djcass44:jmp-auth:1293803642")
    implementation("dev.castive:jmp-auth:0.3.3")
    implementation("com.github.djcass44:log2:3.3")
    implementation("com.github.djcass44:fav2:v0.2.1")
    implementation("com.github.djcass44:eventlog:72b6dac4e2")

    implementation("io.javalin:javalin:2.8.0")
    implementation("com.corundumstudio.socketio:netty-socketio:1.7.17")

    implementation("org.slf4j:slf4j-simple:1.7.25")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
    implementation("com.beust:klaxon:5.0.1")
    implementation("info.debatty:java-string-similarity:1.1.0")
    implementation("com.amdelamar:jhash:2.1.0")
    implementation("io.github.rybalkinsd:kohttp:0.7.1")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.jsoup:jsoup:1.11.3")

    implementation("commons-cli:commons-cli:1.4")

    implementation("com.auth0:java-jwt:3.7.0")
    implementation("com.github.kmehrunes:javalin-jwt:v0.1")

    implementation("org.jetbrains.exposed:exposed:0.11.2")
    implementation("com.zaxxer:HikariCP:3.3.1")

    // JDBC drivers (only includes those supported by github.com/JetBrains/Exposed)
    runtimeOnly("org.xerial:sqlite-jdbc:3.25.2")
    runtimeOnly("org.postgresql:postgresql:42.2.2")
    runtimeOnly("mysql:mysql-connector-java:8.0.15")
    runtimeOnly("com.h2database:h2:1.4.199")
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc:7.3.0.jre11-preview")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.2.0")
}

application {
    mainClassName = "dev.castive.jmp.EntrypointKt"
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<ShadowJar> {
    archiveName = "jmp.$extension"
}
tasks.withType<Test> {
    useJUnitPlatform()
}
task("buildPackage") {
    println("Building package...")
    finalizedBy("increment-patch", "shadowJar")
}