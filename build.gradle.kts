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
	kotlin("jvm") version "1.3.60"
	id("com.github.johnrengelman.shadow") version "5.1.0"
	id("com.github.ben-manes.versions") version "0.25.0"
	application
	jacoco
}

group = "dev.castive"
version = "2.1"

apply(plugin = "java")

ant.importBuild("version.xml")

repositories {
	maven(url = "https://jitpack.io")
	mavenCentral()
	jcenter()
//    mavenLocal()
}

val junitVersion: String by project
val jettyVersion: String by project

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect")


	implementation("com.github.djcass44:jmp-auth:a923eb2c30")
//    implementation("dev.castive:jmp-auth:0.6.6")
	implementation("com.github.djcass44:simpleconfig:0.1-alpha1")
	implementation("com.github.djcass44:log2:3.4")
	implementation("com.github.djcass44:castive-utilities:v3")

	implementation("io.javalin:javalin:3.6.0")
	// http2
	implementation("org.eclipse.jetty.http2:http2-server:$jettyVersion")
	implementation("org.eclipse.jetty:jetty-alpn-conscrypt-server:$jettyVersion")
	implementation("org.eclipse.jetty.alpn:alpn-api:1.1.3.v20160715")
	implementation("org.mortbay.jetty.alpn:alpn-boot:8.1.13.v20181017")

	implementation("org.slf4j:slf4j-simple:1.7.26")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.0")
	implementation("info.debatty:java-string-similarity:1.2.1")
	implementation("com.amdelamar:jhash:2.1.0")
	implementation("com.google.code.gson:gson:2.8.5")
	implementation("org.jsoup:jsoup:1.12.1")
	implementation("com.google.guava:guava:28.1-jre")

	implementation("com.squareup.okhttp3:okhttp:4.2.0")

	implementation("commons-cli:commons-cli:1.4")

	implementation("com.auth0:java-jwt:3.7.0")
	implementation("com.github.kmehrunes:javalin-jwt:0.2")
	implementation("com.github.scribejava:scribejava-apis:6.8.1")

	implementation("org.jetbrains.exposed:exposed:0.17.4")
	implementation("com.zaxxer:HikariCP:3.4.1")

	// swagger
	implementation("io.swagger.core.v3:swagger-core:2.0.8")
	implementation("org.webjars:swagger-ui:3.23.8")

	// Crypto providers
	implementation("com.amazonaws:aws-java-sdk-ssm:1.11.642")


	// JDBC drivers (only includes those supported by github.com/JetBrains/Exposed)
	runtimeOnly("org.xerial:sqlite-jdbc:3.28.0") // tested (django)
	runtimeOnly("org.postgresql:postgresql:42.2.8") // tested (django)
	runtimeOnly("mysql:mysql-connector-java:8.0.17") // untested
	runtimeOnly("com.h2database:h2:1.4.199") // untested
	runtimeOnly("com.microsoft.sqlserver:mssql-jdbc:7.4.1.jre12") // untested

	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

	testImplementation("org.hamcrest:hamcrest:2.2")
	testImplementation("org.mockito:mockito-core:3.0.0")
	testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
	testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
	mainClassName = "dev.castive.jmp.EntrypointKt"
	applicationDefaultJvmArgs = listOf(
		"-Djava.util.logging.config.file=src/main/resources/logging.properties"
	)
}

tasks {
	wrapper {
		gradleVersion = "5.6.4"
		distributionType = Wrapper.DistributionType.ALL
	}
	withType<KotlinCompile>().all {
		kotlinOptions.jvmTarget = "11"
	}
	withType<ShadowJar> {
		baseName = "jmp"
		classifier = null
		version = null
		mergeServiceFiles()
	}
	withType<Test> {
		useJUnitPlatform()
	}
	withType<JacocoReport> {
		reports {
			xml.isEnabled = true
		}
	}
}
jacoco {
	toolVersion = "0.8.4"
}
task("buildPackage") {
	println("Building package...")
	finalizedBy("increment-patch", "shadowJar")
}
val codeCoverageReport by tasks.creating(JacocoReport::class) {
	dependsOn("test")
}
