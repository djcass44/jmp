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

import com.django.log2.logging.Log
import dev.castive.jmp.api.Auth
import dev.castive.jmp.auth.provider.BaseProvider
import dev.castive.jmp.auth.provider.InternalProvider
import dev.castive.jmp.auth.provider.LDAPProvider
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Providers(config: ConfigStore, private val auth: Auth) {
    companion object {
        val internalProvider = InternalProvider()
        var primaryProvider: BaseProvider? = null

        const val PROP_LDAP = "ldap"
        const val PROP_LDAP_HOST = "ldap.host"
        const val PROP_LDAP_PORT = "ldap.port"
        const val PROP_LDAP_CTX = "ldap.context"
        const val PROP_LDAP_USER = "ldap.user"
        const val PROP_LDAP_PASS = "ldap.password"
        const val PROP_LDAP_USER_FILTER = "jmp.ldap.user_query"
        const val PROP_LDAP_USER_ID = "jmp.ldap.user_uid"

        const val PROP_LDAP_RM_STALE = "jmp.ldap.remove_stale"
        const val PROP_LDAP_SYNC = "jmp.ldap.sync_rate"

        const val PROP_EXT_ALLOW_LOCAL = "jmp.ext.allow_local"
    }
    val properties = Properties()

    val keyedProps = HashMap<String, String>()

    init {
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

        initLDAP()
//        startCRON()
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
                "$PROP_LDAP_USER_ID=uid\n" +
                "$PROP_LDAP_RM_STALE=true\n" +
                "$PROP_LDAP_SYNC=300000\n" +
                "$PROP_EXT_ALLOW_LOCAL=true",
            StandardCharsets.UTF_8)
    }

    /**
     * Try to setup LDAP provider if it's enabled
     */
    private fun initLDAP() {
        // Process non-ldap properties before ldap check
        keyedProps[PROP_EXT_ALLOW_LOCAL] = properties.getOrDefault(PROP_EXT_ALLOW_LOCAL, "true").toString()
        val useLDAP = properties.getOrDefault(PROP_LDAP, "false").toString().toBoolean()
        Log.v(javaClass, "LDAP: $useLDAP")
        if(!useLDAP) return
        val ldapHost = properties[PROP_LDAP_HOST].toString()
        val ldapPort = properties.getOrDefault(PROP_LDAP_PORT, 389).toString().toInt()
        val ldapContext = properties[PROP_LDAP_CTX].toString()
        val ldapUser = properties[PROP_LDAP_USER].toString()
        val ldapPassword = properties[PROP_LDAP_PASS].toString()
        keyedProps[PROP_LDAP_RM_STALE] = properties.getOrDefault(PROP_LDAP_RM_STALE, "true").toString()
        keyedProps[PROP_LDAP_SYNC] = properties.getOrDefault(PROP_LDAP_SYNC, (300 * 1000).toLong()).toString()
        Log.i(javaClass, "Using LDAP sync rate: ${keyedProps[PROP_LDAP_SYNC]!!.toLong()} milliseconds")
        if(keyedProps[PROP_LDAP_SYNC] == null) keyedProps[PROP_LDAP_SYNC] = "300000" // Default to 5 minutes
        if((keyedProps[PROP_LDAP_SYNC]!!.toLong()) < 5000) {
            Log.w(javaClass, "LDAP sync rate must be above 5000")
            keyedProps[PROP_LDAP_SYNC] = "5000"
        }
        keyedProps[PROP_LDAP_USER_FILTER] = properties[PROP_LDAP_USER_FILTER].toString()
        keyedProps[PROP_LDAP_USER_ID] = properties[PROP_LDAP_USER_ID].toString()

        primaryProvider = LDAPProvider(ldapHost, ldapPort, ldapContext, ldapUser, ldapPassword, keyedProps[PROP_LDAP_USER_FILTER].toString(), keyedProps[PROP_LDAP_USER_ID].toString())

        startCRON()
    }

    private fun startCRON() = fixedRateTimer(javaClass.name, true, 0, (keyedProps[PROP_LDAP_SYNC]!!.toLong())) { sync() }

    private fun sync() {
        if(primaryProvider == null) {
            Log.i(javaClass, "Skipping user sync, no provider setup")
            return
        }
        Log.i(javaClass, "Running batch update using ${primaryProvider!!::class.java.name}")
        primaryProvider!!.setup()
        val users = primaryProvider!!.getUsers()
        if(users == null) {
            Log.w(javaClass, "External provider: ${primaryProvider?.getName()} returned null, perhaps it's not connected yet?")
            return
        }
        Log.i(javaClass, "External provider: ${primaryProvider?.getName()} found ${users.size} users")
        val names = arrayListOf<String>()
        transaction {
            users.forEach { u ->
                names.add(u.username)
                val match = User.find { Users.username eq u.username and Users.from.eq(LDAPProvider.SOURCE_NAME) }
                if (match.empty()) {
                    // User doesn't exist yet
                    User.new {
                        username = u.username
                        hash = ""
                        token = UUID.randomUUID()
                        role = auth.getDAOUserRole()
                        from = u.from
                    }
                }
                else match.elementAt(0).apply {
                    // TODO update user details
//                    role = if (admin) auth.getDAOAdminRole() else auth.getDAOUserRole()
                }
            }
            // Get LDAP user which weren't in the most recent search and delete them
            val externalUsers = User.find { Users.from eq LDAPProvider.SOURCE_NAME }
            val invalid = arrayListOf<User>()
            externalUsers.forEach {
                if (!names.contains(it.username))
                    invalid.add(it)
            }
            Log.i(javaClass, "Found ${invalid.size} stale users")
            if((keyedProps.getOrDefault(PROP_LDAP_RM_STALE, "false").toBoolean())) {
                invalid.forEach { it.delete() }
                if (invalid.size > 0) Log.w(javaClass, "Removed ${invalid.size} stale users")
            }
            else Log.i(javaClass, "Stale user removal blocked by application policy")
        }
    }
}