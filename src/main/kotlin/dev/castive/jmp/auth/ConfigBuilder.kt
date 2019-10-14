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

import dev.castive.javalin_auth.auth.connect.CrowdConfig
import dev.castive.javalin_auth.auth.connect.LDAPConfig
import dev.castive.javalin_auth.auth.connect.LDAPConfig2
import dev.castive.javalin_auth.auth.connect.MinimalConfig
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.BasicAuthentication
import dev.castive.jmp.io.DataProvider
import dev.castive.jmp.util.json
import dev.castive.jmp.util.parse
import dev.castive.log2.*
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class ConfigBuilder {
	private val configVersion = "2019-10-02"

	data class JMPConfiguration(
		val version: String,
		val realm: String,
		val min: MinimalConfig,
		val ldap: LDAPConfiguration,
		val crowdUrl: String
	) {
		fun asLDAP2(): LDAPConfig2 = LDAPConfig2(
			min,
			ldap.core,
			LDAPConfig.Extras(
				ldap.userFilter,
				ldap.uid,
				ldap.reconnectOnAuth,
				min.removeStale,
				min.syncRate,
				min.blockLocal,
				min.maxConnectAttempts
			),
			ldap.groups
		)
		fun asCrowd(): CrowdConfig = CrowdConfig(
			min,
			crowdUrl
		)
	}

	data class LDAPConfiguration(
		val core: LDAPConfig,
		val userFilter: String,
		val uid: String,
		val reconnectOnAuth: Boolean,
		val groups: LDAPConfig.Groups
	)

	/**
	 * Builds a JMPConfiguration with default values
	 * Most of these values will be empty as they are not generic
	 */
	internal fun getDefault(): JMPConfiguration = JMPConfiguration(
		"2019-10-02",
		"db",
		MinimalConfig(false, BasicAuthentication("username", "password")),
		LDAPConfiguration(
			LDAPConfig(server = "localhost", contextDN = ""),
			"",
			"",
			false,
			LDAPConfig.Groups("", "", "")
		),
		"http://localhost:8095/crowd"
	)

	internal fun getDataFile(): File? = DataProvider.get("jmp.json") ?: kotlin.run {
		"Unable to allocate jmp.json file, returning defaults".logf(javaClass)
		return null
	}

	fun get(): JMPConfiguration {
		val data = getDataFile() ?: return getDefault()
		Log.v(javaClass, "JSON properties file exists: ${data.exists()}")
		// read in the JSON
		val config = if(data.exists()) {
			"Reading JMP configuration from disk: ${data.absolutePath}".logok(javaClass)
			data.readText(StandardCharsets.UTF_8).parse(JMPConfiguration::class.java)
		}
		else {
			try {
				"Attempting to create new jmp.json file".logi(javaClass)
				data.createNewFile()
				Log.i(javaClass, "Created properties file in ${data.absolutePath}")
				writeDefaults(data)
			} catch (e: IOException) {
				"Failed to create jmp.json file, default values will be used until next restart".loge(javaClass)
				Log.e(javaClass, "Failed to setup properties: $e, ${data.absolutePath}")
			}
			getDefault()
		}
		return if(validateConfig(config)) {
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
	private fun writeDefaults(file: File) {
		file.writeText(getDefault().json(), StandardCharsets.UTF_8)
	}
}