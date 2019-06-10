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
import dev.castive.jmp.db.ConfigStore
import dev.castive.log2.Log
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

class LDAPConfigBuilder(private val config: ConfigStore) {
	companion object {
		const val PROP_TYPE = "type"
		const val PROP_LDAP = "ldap"
		const val PROP_LDAP_HOST = "ldap.host"
		const val PROP_LDAP_PORT = "ldap.port"
		const val PROP_LDAP_CTX = "ldap.context"
		const val PROP_LDAP_USER = "ldap.user"
		const val PROP_LDAP_PASS = "ldap.password"
		const val PROP_LDAP_USER_FILTER = "jmp.ldap.user_query"
		const val PROP_LDAP_GROUP_FILTER = "jmp.ldap.group_filter"
		const val PROP_LDAP_GROUP_QUERY = "jmp.ldap.group_query"
		const val PROP_LDAP_USER_ID = "jmp.ldap.user_uid"
		const val PROP_LDAP_GROUP_ID = "jmp.ldap.group_uid"
		const val PROP_LDAP_MAX_FAILURE = "jmp.ldap.max_failure"
		const val PROP_LDAP_AUTH_RECONNECT = "jmp.ldap.auth_reconnect"

		const val PROP_LDAP_RM_STALE = "jmp.ldap.remove_stale"
		const val PROP_LDAP_SYNC = "jmp.ldap.sync_rate"

		const val PROP_EXT_BLOCK_LOCAL = "jmp.ext.block_local"

		const val PROP_CROWD = "crowd"
		const val PROP_CROWD_URL = "crowd.url"
		const val PROP_CROWD_USER = "crowd.user"
		const val PROP_CROWD_PASS = "crowd.pass"
	}
	val properties = Properties()

	lateinit var type: String
		private set

	lateinit var min: MinimalConfig
		private set
	lateinit var ldapConfig: LDAPConfig2
		private set
	private lateinit var core: LDAPConfig
	private lateinit var extra: LDAPConfig.Extras
	private lateinit var group: LDAPConfig.Groups

	lateinit var crowdConfig: CrowdConfig
		private set

	init {
		validateFile()
		compute()
	}
	private fun validateFile() {
		val data = File(config.dataPath, "jmp.properties")
		Log.v(javaClass, "Properties file exists: ${data.exists()}")
		if(data.exists()) properties.load(data.inputStream())
		else {
			try {
				data.createNewFile()
				Log.i(javaClass, "Created properties file in ${data.absolutePath}")
				writeDefaults(data)
			}
			catch (e: IOException) {
				Log.e(javaClass, "Failed to setup properties: $e, ${data.absolutePath}")
			}
		}
	}
	private fun writeDefaults(file: File) {
		file.writeText(
					"$PROP_TYPE=ldap\n" +
					"$PROP_LDAP=false\n" +
					"$PROP_LDAP_HOST=localhost\n" +
					"$PROP_LDAP_PORT=389\n" +
					"$PROP_LDAP_CTX=\n" +
					"$PROP_LDAP_USER=admin\n" +
					"$PROP_LDAP_PASS=password\n" +
					"$PROP_LDAP_USER_FILTER=\n" +
					"$PROP_LDAP_GROUP_FILTER=\n" +
					"$PROP_LDAP_GROUP_QUERY=\n" +
					"$PROP_LDAP_USER_ID=uid\n" +
					"$PROP_LDAP_GROUP_ID=\n" +
					"$PROP_LDAP_MAX_FAILURE=5\n" +
					"$PROP_LDAP_AUTH_RECONNECT=false" +
					"$PROP_LDAP_RM_STALE=true\n" +
					"$PROP_LDAP_SYNC=300000\n" +
					"$PROP_EXT_BLOCK_LOCAL=false" +
					"$PROP_CROWD=false\n" +
					"$PROP_CROWD_USER=jumpuser\n" +
					"$PROP_CROWD_PASS=password\n" +
					"$PROP_CROWD_URL=http://localhost:8095/crowd\n",
			StandardCharsets.UTF_8)
	}
	private fun compute() {
		type = properties.getOrDefault(PROP_TYPE, "ldap").toString()
		val enableProp = when(type) {
			"crowd" -> PROP_CROWD
			else -> PROP_LDAP
		}
		val userProp = when(type) {
			"crowd" -> PROP_CROWD_USER
			else -> PROP_LDAP_USER
		}
		val passProp = when(type) {
			"crowd" -> PROP_CROWD_PASS
			else -> PROP_LDAP_PASS
		}
		min = MinimalConfig(
			enabled = properties.getOrDefault(enableProp, false).toString().toBoolean(),
			serviceAccount = BasicAuthentication(properties[userProp].toString(), properties[passProp].toString()),
			syncRate = properties.getOrDefault(PROP_LDAP_SYNC, 300000).toString().toLongOrNull() ?: 300000,
			maxConnectAttempts = properties.getOrDefault(PROP_LDAP_MAX_FAILURE, 5).toString().toIntOrNull() ?: 5,
			removeStale = properties.getOrDefault(PROP_LDAP_RM_STALE, true).toString().toBoolean(),
			blockLocal =  properties.getOrDefault(PROP_EXT_BLOCK_LOCAL, false).toString().toBoolean()
		)
		if(type == "ldap") {
			core = LDAPConfig(
				server = properties.getOrDefault(PROP_LDAP_HOST, "localhost").toString(),
				port = properties.getOrDefault(PROP_LDAP_PORT, 389).toString().toIntOrNull() ?: 389,
				contextDN = properties[PROP_LDAP_CTX].toString()
			)
			extra = LDAPConfig.Extras(
				userFilter = properties[PROP_LDAP_USER_FILTER].toString(),
				uid = properties[PROP_LDAP_USER_ID].toString(),
				reconnectOnAuth = properties.getOrDefault(PROP_LDAP_AUTH_RECONNECT, false).toString().toBoolean()
			)
			group = LDAPConfig.Groups(
				groupFilter = properties[PROP_LDAP_GROUP_FILTER].toString(),
				groupQuery = properties[PROP_LDAP_GROUP_QUERY].toString(),
				gid = properties[PROP_LDAP_GROUP_ID].toString()
			)
			ldapConfig = LDAPConfig2(
				min,
				baseConfig = core,
				extraConfig = extra,
				groupConfig = group
			)
		}
		else if(type == "crowd") {
			crowdConfig = CrowdConfig(
				min,
				crowdUrl = properties.getOrDefault(PROP_CROWD_URL, "http://localhost:8095/crowd").toString()
			)
		}
		if(min.blockLocal) Log.w(javaClass, "Local account creation is disabled by application policy")
	}
}