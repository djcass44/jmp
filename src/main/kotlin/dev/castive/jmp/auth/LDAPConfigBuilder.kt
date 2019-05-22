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

import dev.castive.javalin_auth.auth.connect.LDAPConfig
import dev.castive.jmp.db.ConfigStore
import dev.castive.log2.Log
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

class LDAPConfigBuilder(private val config: ConfigStore) {
	companion object {

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
	}
	val properties = Properties()

	lateinit var core: LDAPConfig
		private set
	lateinit var extra: LDAPConfig.Extras
		private set
	lateinit var group: LDAPConfig.Groups
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
					"$PROP_EXT_BLOCK_LOCAL=false",
			StandardCharsets.UTF_8)
	}
	private fun compute() {
		core = LDAPConfig(
			enabled = properties.getOrDefault(PROP_LDAP, false).toString().toBoolean(),
			server = properties.getOrDefault(PROP_LDAP_HOST, "localhost").toString(),
			port = properties.getOrDefault(PROP_LDAP_PORT, 389).toString().toIntOrNull() ?: 389,
			contextDN = properties[PROP_LDAP_CTX].toString(),
			serviceUserDN = properties[PROP_LDAP_USER].toString(),
			serviceUserPassword = properties[PROP_LDAP_PASS].toString()
		)
		extra = LDAPConfig.Extras(
			userFilter = properties[PROP_LDAP_USER_FILTER].toString(),
			removeStale = properties.getOrDefault(PROP_LDAP_RM_STALE, true).toString().toBoolean(),
			syncRate = properties.getOrDefault(PROP_LDAP_SYNC, 300000).toString().toLongOrNull() ?: 300000,
			uid = properties[PROP_LDAP_USER_ID].toString(),
			blockLocal =  properties.getOrDefault(PROP_EXT_BLOCK_LOCAL, false).toString().toBoolean(),
			maxConnectAttempts = properties.getOrDefault(PROP_LDAP_MAX_FAILURE, 5).toString().toIntOrNull() ?: 5,
			reconnectOnAuth = properties.getOrDefault(PROP_LDAP_AUTH_RECONNECT, false).toString().toBoolean()
		)
		group = LDAPConfig.Groups(
			groupFilter = properties[PROP_LDAP_GROUP_FILTER].toString(),
			groupQuery = properties[PROP_LDAP_GROUP_QUERY].toString(),
			gid = properties[PROP_LDAP_GROUP_ID].toString()
		)
		if(extra.blockLocal) Log.w(javaClass, "Local account creation is disabled by application policy")
	}
}