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
import org.ajoberstar.grgit.Grgit
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sonarqube.gradle.SonarQubeTask

plugins {
	kotlin("jvm") version "1.3.50"
	id("com.github.johnrengelman.shadow") version "4.0.3"
	application
	jacoco
	id("org.sonarqube") version "2.7.1"
	id("org.ajoberstar.grgit") version "1.7.2"
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
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.2")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.50")

	implementation("com.github.djcass44:jmp-auth:176931eec3")
//    implementation("dev.castive:jmp-auth:0.5.3")
	implementation("com.github.djcass44:log2:7d9def02d1")
	implementation("com.github.djcass44:eventlog:72b6dac4e2")

	implementation("io.javalin:javalin:3.5.0")
	// http2
	implementation("org.eclipse.jetty.http2:http2-server:$jettyVersion")
	implementation("org.eclipse.jetty:jetty-alpn-conscrypt-server:$jettyVersion")
	implementation("org.eclipse.jetty.alpn:alpn-api:1.1.3.v20160715")
	implementation("org.mortbay.jetty.alpn:alpn-boot:8.1.13.v20181017")

	implementation("org.slf4j:slf4j-simple:1.7.26")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.8")
	implementation("info.debatty:java-string-similarity:1.1.0")
	implementation("com.amdelamar:jhash:2.1.0")
	implementation("com.google.code.gson:gson:2.8.5")
	implementation("org.jsoup:jsoup:1.11.3")
	implementation("com.google.guava:guava:28.0-jre")

	implementation("com.squareup.okhttp3:okhttp:4.0.1")

	implementation("commons-cli:commons-cli:1.4")

	implementation("com.auth0:java-jwt:3.7.0")
	implementation("com.github.kmehrunes:javalin-jwt:v0.1")
	implementation("com.github.scribejava:scribejava-apis:6.7.0")

	implementation("org.jetbrains.exposed:exposed:0.16.3")
	implementation("com.zaxxer:HikariCP:3.3.1")

	// Crypto providers
	implementation("com.amazonaws:aws-java-sdk-ssm:1.11.595")


	// JDBC drivers (only includes those supported by github.com/JetBrains/Exposed)
	runtimeOnly("org.xerial:sqlite-jdbc:3.25.2") // tested (django)
	runtimeOnly("org.postgresql:postgresql:42.2.2") // tested (django)
	runtimeOnly("mysql:mysql-connector-java:8.0.15") // untested
	runtimeOnly("com.h2database:h2:1.4.199") // untested
	runtimeOnly("com.microsoft.sqlserver:mssql-jdbc:7.3.0.jre11-preview") // untested

	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

	testImplementation("org.mockito:mockito-core:3.0.0")
	testImplementation("org.jetbrains.kotlin:kotlin-test:1.3.50")
}

application {
	mainClassName = "dev.castive.jmp.EntrypointKt"
	applicationDefaultJvmArgs = listOf(
		"-Djava.util.logging.config.file=src/main/resources/logging.properties"
	)
}

tasks {
	wrapper {
		gradleVersion = "5.5.1"
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
	withType<SonarQubeTask> { dependsOn("test", "jacocoTestReport") }
}
jacoco {
	toolVersion = "0.8.4"
}
task("buildPackage") {
	println("Building package...")
	finalizedBy("increment-patch", "shadowJar")
}
val codeCoverageReport by tasks.creating(JacocoReport::class) { dependsOn("test") }

sonarqube {
	val git = runCatching {Grgit.open(project.rootDir)}.getOrNull()
	// Don't run an analysis if we can't get git context
	val name = (if(git == null) null else runCatching {git.branch.current.name}.getOrNull())
	val target = when(name) {
		null -> null
		"develop" -> "master"
		else -> "develop"
	}
	val branch = if(name != null && target != null) Pair(name, target) else null
	this.isSkipProject = branch == null
	properties{
		property("sonar.projectKey", "djcass44:jmp")
		property("sonar.projectName", "djcass44/jmp")
//		if(branch != null) {
//			property("sonar.branch.name", branch.first)
//			property("sonar.branch.target", branch.second)
//		}
		property("sonar.junit.reportsPath", "$projectDir/build/test-results")
	}
}