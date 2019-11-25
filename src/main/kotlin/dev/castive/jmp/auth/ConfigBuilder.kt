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

package dev.castive.jmp.auth

import dev.castive.javalin_auth.config.Crowd2Config
import dev.castive.javalin_auth.config.LDAP2Config
import dev.castive.javalin_auth.config.OAuth2Config
import dev.castive.jmp.config.DataConfig
import dev.castive.jmp.config.JMPConfig
import dev.castive.jmp.config.ServerConfig
import dev.castive.jmp.io.DataProvider
import dev.castive.log2.*
import dev.dcas.simpleconfig.ConfigLoader
import java.io.File
import java.io.IOException

class ConfigBuilder {
	private val configVersion = "2019-10-02"

	data class JMPConfiguration(
		val version: String,
		val blockLocal: Boolean,
		val jmp: JMPConfig,
		val crowd: Crowd2Config,
		val ldap: LDAP2Config,
		val oauth2: Map<String, OAuth2Config> = mapOf()
	)

	/**
	 * Builds a JMPConfiguration with default values
	 * Most of these values will be empty as they are not generic
	 */
	internal fun getDefault(): JMPConfiguration = JMPConfiguration(
		"2019-10-02",
		false,
		JMPConfig(
			ServerConfig(
				7000,
				false,
				"",
				"",
				false
			),
			DataConfig(
				"jdbc:sqlite:jmp.db",
				"org.sqlite.JDBC"
			)
		),
		Crowd2Config(
			false,
			"http://localhost:8095/crowd",
			"user",
			"hunter2"
		),
		LDAP2Config(
			false,
			"localhost",
			389,
			"dc=example,dc=org",
			"uid",
			"user",
			"hunter2"
		),
		mapOf()
	)

	internal fun getDataFile(): File? = DataProvider.get("jmp.yaml") ?: kotlin.run {
		"Unable to allocate jmp.yaml file, returning defaults".logf(javaClass)
		return null
	}

	fun get(): JMPConfiguration {
		val data = getDataFile() ?: return getDefault()
		Log.v(javaClass, "JSON properties file exists: ${data.exists()}")
		// read in the JSON
		val config = if(data.exists()) {
			"Reading JMP configuration from disk: ${data.absolutePath}".logok(javaClass)
			ConfigLoader(JMPConfiguration::class.java).load(data.absolutePath)
		}
		else {
			try {
				"Attempting to create new jmp.yaml file".logi(javaClass)
				data.createNewFile()
				Log.i(javaClass, "Created properties file in ${data.absolutePath}")
				write(data)
			} catch (e: IOException) {
				"Failed to create jmp.yaml file, default values will be used until next restart".loge(javaClass)
				Log.e(javaClass, "Failed to setup properties: $e, ${data.absolutePath}")
			}
			getDefault()
		}
		write(data, config)
		return if(!validateConfig(config)) {
			getDefault()
		}
		else config
	}

	internal fun validateConfig(config: JMPConfiguration): Boolean {
		return if(config.version != configVersion) {
			"JMP configuration has missing or incorrect version: [expected: $configVersion, got: ${config.version}], default values will be used".logf(javaClass)
			false
		} else true
	}

	/**
	 * Write a default JMPConfiguration instance to a file
	 */
	private fun write(file: File) {
		write(file, getDefault())
	}

	/**
	 * Write a configuration to file
	 */
	private fun write(file: File, config: JMPConfiguration) {
		"Writing configuration: ${config.version} to file: ${file.absolutePath}".logi(javaClass)
		ConfigLoader.mapper.writeValue(file, config)
	}
}
