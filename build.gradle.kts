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
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	id("org.springframework.boot") version "2.2.5.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	id("com.github.ben-manes.versions") version "0.27.0"
	kotlin("jvm") version "1.3.70"
	kotlin("plugin.spring") version "1.3.70"
	kotlin("plugin.jpa") version "1.3.70"
	kotlin("kapt") version "1.3.70"
}

group = "dev.castive"
version = "0.5"

java.apply {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

ant.importBuild("version.xml")

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	maven(url = "https://mvn.v2.dcas.dev")
	mavenCentral()
	maven(url = "https://jitpack.io")
	//jcenter()
//    mavenLocal()
}

val junitVersion: String by project
extra["springCloudVersion"] = "Hoxton.SR2"

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation(kotlin("reflect"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.+")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.+")

	implementation("com.sun.activation:javax.activation:1.2.0")

	// spring boot
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-ldap")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-security")
	kapt("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-configuration-processor")

	// spring misc
	implementation("org.springframework.cloud:spring-cloud-starter-config")

	implementation("com.github.djcass44:log2:4.1")
	implementation("com.github.djcass44:castive-utilities:v6.RC3")
	implementation("com.github.djcass44:jmp-security-utils:0.1.RC11")
//	implementation("dev.dcas.jmp.security:core:0.3-SNAPSHOT")
//	implementation("dev.dcas.jmp.security:shim:0.3-SNAPSHOT")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.2")
	implementation("info.debatty:java-string-similarity:1.2.1")
	implementation("org.hibernate:hibernate-search-orm:5.11.5.Final")
	implementation("org.jsoup:jsoup:1.12.1")
	implementation("com.google.guava:guava:28.2-jre")

	// ldap
	implementation("com.unboundid:unboundid-ldapsdk:4.0.14")

	// swagger
	implementation("io.springfox:springfox-swagger2:2.9.2")
	implementation("io.springfox:springfox-swagger-ui:2.9.2")

	// JDBC drivers
	runtimeOnly("org.postgresql:postgresql:42.2.9") // tested (django)
	runtimeOnly("com.h2database:h2:1.4.+")

	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test")

	// rest assured
	val restAssuredVersion = "4.2.0"
	testImplementation("io.rest-assured:json-path:$restAssuredVersion")
	testImplementation("io.rest-assured:xml-path:$restAssuredVersion")
	testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
	testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
	testImplementation("io.rest-assured:spring-mock-mvc:$restAssuredVersion")

	testImplementation("org.hamcrest:hamcrest:2.2")
	testImplementation("org.mockito:mockito-core:3.2.4")
	testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
	implementation(kotlin("script-runtime"))
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

configure<JavaPluginConvention> {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

springBoot {
	buildInfo()
}

tasks {
	wrapper {
		gradleVersion = "6.1"
		distributionType = Wrapper.DistributionType.ALL
	}
	withType<KotlinCompile>().all {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = "11"
		}
	}
	withType<Test> {
		useJUnitPlatform()
	}
	withType<BootJar> {
		archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
	}
}
