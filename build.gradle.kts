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

import dev.dcas.gradle.boot
import dev.dcas.gradle.cloud
import dev.dcas.gradle.kotlinx
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	id("org.springframework.boot") version "2.4.1"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	id("com.github.ben-manes.versions") version "0.31.0"
	id("dev.dcas.gradle-util") version "0.1"
	kotlin("jvm") version "1.4.21"
	kotlin("plugin.spring") version "1.4.21"
	kotlin("plugin.jpa") version "1.4.21"
	kotlin("kapt") version "1.4.21"
}

group = "dev.castive"
version = "0.6.5"

java.apply {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	maven(url = "https://prism.dcas.dev/maven")
	mavenCentral()
	maven(url = "https://jitpack.io")
	//jcenter()
//    mavenLocal()
}

val junitVersion: String by project
extra["springCloudVersion"] = "2020.0.0"

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation(kotlin("reflect"))
	implementation(kotlinx("coroutines-core:1.4.2"))
	implementation(kotlinx("coroutines-jdk8:1.4.2"))

	val ktorVersion = "1.4.0"
	implementation("io.ktor:ktor-client-apache:$ktorVersion")
	testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")

	implementation("com.sun.activation:javax.activation:1.2.0")

	// spring boot
	implementation(boot("starter-data-jpa"))
	implementation(boot("starter-data-ldap"))
	implementation(boot("starter-web"))
	implementation(boot("starter-websocket"))
	implementation(boot("starter-cache"))
	implementation(boot("starter-actuator"))
	implementation(boot("starter-security"))
	kapt(boot("configuration-processor"))
	implementation(boot("configuration-processor"))

	// spring misc
	implementation(cloud("starter-config"))

	implementation("com.github.djcass44:log2:4.1")
	implementation("com.github.djcass44:castive-utilities:v6.RC3")
	implementation("com.github.djcass44:jmp-security-utils:0.1.3-beta.2-1")
//	implementation("dev.dcas.jmp.security:core:0.1.3-SNAPSHOT")
//	implementation("dev.dcas.jmp.security:shim:0.1.3-SNAPSHOT")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")
	implementation("info.debatty:java-string-similarity:1.2.1")
	implementation("org.hibernate:hibernate-search-orm:5.11.7.Final")
	implementation("com.google.guava:guava:29.0-jre")

	// ldap
	implementation("com.unboundid:unboundid-ldapsdk:4.0.14")

	// swagger
	implementation("io.springfox:springfox-swagger2:2.9.2")
	implementation("io.springfox:springfox-swagger-ui:2.9.2")

	// JDBC drivers
	runtimeOnly("org.postgresql:postgresql:42.2.16") // tested (django)
	runtimeOnly("com.h2database:h2:1.4.+")

	implementation("io.sentry:sentry-spring:1.7.30")

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
	testImplementation("org.mockito:mockito-core:3.5.10")
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

// system properties to pull into /actuator/info
val props: List<String> = listOf(
	"java.vendor",
	"java.version",
	"os.arch",
	"os.name",
	"os.version"
)

springBoot {
	buildInfo {
		properties {
			// add some additional props
			additional = mutableMapOf()
			props.forEach {
				additional[it] = System.getProperty(it, "")
			}
			// only set in CI build
			additional["git.commit"] = System.getenv("CI_COMMIT_SHA") ?: ""
		}
	}
}

tasks {
	wrapper {
		gradleVersion = "6.7.1"
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
