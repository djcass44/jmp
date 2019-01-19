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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.20-eap-52"
}

group = "com.django"
version = "1.0-SNAPSHOT"

apply(plugin = "java")

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven(url = "https://dl.bintray.com/kotlin/exposed")
    maven(url = "https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.gitlab.django-sandbox:log2:8b941edd1a")

    implementation("io.javalin:javalin:2.6.0")
    implementation("org.slf4j:slf4j-simple:1.7.25")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")

    implementation("org.jetbrains.exposed:exposed:0.11.2")
    runtimeOnly("org.xerial:sqlite-jdbc:3.25.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}