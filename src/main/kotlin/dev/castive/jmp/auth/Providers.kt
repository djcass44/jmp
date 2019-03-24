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
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Providers(config: ConfigStore, private val auth: dev.castive.jmp.api.Auth) {
    companion object {
        val internalProvider = InternalProvider()
        var primaryProvider: BaseProvider? = null

        const val PROP_LDAP = "ldap"
        const val PROP_LDAP_HOST = "ldap.host"
        const val PROP_LDAP_PORT = "ldap.port"
        const val PROP_LDAP_CTX = "ldap.context"
        const val PROP_LDAP_USER = "ldap.user"
        const val PROP_LDAP_PASS = "ldap.password"

        const val PROP_LDAP_RM_STALE = "jmp.ldap.remove_stale"
        const val PROP_LDAP_SYNC = "jmp.ldap.sync_rate"

        const val PROP_EXT_ALLOW_LOCAL = "jmp.ext.allow_local"
    }
    val properties = Properties()

    val keyedProps = HashMap<String, Any>()

    init {
        val data = File(config.dataPath, "jmp.properties")
        Log.v(javaClass, "Properties file exists: ${data.exists()}")
        if(data.exists()) properties.load(data.inputStream())
        else {
            try {
                data.createNewFile()
                Log.i(javaClass, "Created properties file in ${data.absolutePath}")
            }
            catch (e: IOException) {
                Log.e(javaClass, "Failed to setup properties: $e, ${data.absolutePath}")
            }
        }

        initLDAP()
        startCRON()
    }

    /**
     * Try to setup LDAP provider if it's enabled
     */
    private fun initLDAP() {
        val useLDAP = properties.getOrDefault(PROP_LDAP, "false").toString().toBoolean()
        Log.v(javaClass, "LDAP: $useLDAP")
        if(!useLDAP) return
        val ldapHost = properties[PROP_LDAP_HOST].toString()
        val ldapPort = properties.getOrDefault(PROP_LDAP_PORT, 389).toString().toInt()
        val ldapContext = properties[PROP_LDAP_CTX].toString()
        val ldapUser = properties[PROP_LDAP_USER].toString()
        val ldapPassword = properties[PROP_LDAP_PASS].toString()
        keyedProps[PROP_LDAP_RM_STALE] = properties.getOrDefault(PROP_LDAP_RM_STALE, "true").toString().toBoolean()
        keyedProps[PROP_LDAP_SYNC] = properties.getOrDefault(PROP_LDAP_SYNC, (300 * 1000).toLong()).toString().toLong()
        Log.i(javaClass, "Using LDAP sync rate: ${keyedProps[PROP_LDAP_SYNC] as Long} milliseconds")
        if((keyedProps[PROP_LDAP_SYNC] as Long) < 5000) {
            Log.w(javaClass, "LDAP sync rate must be above 5000")
            keyedProps[PROP_LDAP_SYNC] = 5000L
        }
        keyedProps[PROP_EXT_ALLOW_LOCAL] = properties.getOrDefault(PROP_EXT_ALLOW_LOCAL, "true").toString().toBoolean()

        primaryProvider = LDAPProvider(ldapHost, ldapPort, ldapContext, ldapUser, ldapPassword)
    }

    private fun startCRON() = fixedRateTimer(javaClass.name, true, 0, (keyedProps[PROP_LDAP_SYNC] as Long)) { sync() }

    private fun sync() {
        if(primaryProvider == null) {
            Log.i(javaClass, "Skipping user sync, no provider setup")
            return
        }
        Log.i(javaClass, "Running batch update using ${primaryProvider!!::class.java.name}")
        primaryProvider!!.setup()
        val users = primaryProvider!!.getUsers()
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
            if((keyedProps[PROP_LDAP_RM_STALE] as Boolean)) {
                invalid.forEach { it.delete() }
                if (invalid.size > 0) Log.w(javaClass, "Removed ${invalid.size} stale users")
            }
            else Log.i(javaClass, "Stale user remove blocked by application property")
        }
    }
}