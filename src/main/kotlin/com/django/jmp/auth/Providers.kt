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

package com.django.jmp.auth

import com.django.jmp.api.Auth
import com.django.jmp.auth.provider.BaseProvider
import com.django.jmp.auth.provider.InternalProvider
import com.django.jmp.auth.provider.LDAPProvider
import com.django.jmp.db.ConfigStore
import com.django.jmp.db.dao.User
import com.django.jmp.db.dao.Users
import com.django.log2.logging.Log
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.concurrent.fixedRateTimer

class Providers(config: ConfigStore, private val auth: Auth) {
    companion object {
        val internalProvider = InternalProvider()
        var primaryProvider: BaseProvider? = null
    }
    private val properties = Properties()
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
        val useLDAP = properties.getOrDefault("ldap", "false").toString().toBoolean()
        Log.v(javaClass, "LDAP: $useLDAP")
        if(!useLDAP) return
        val ldapHost = properties["ldap.host"].toString()
        val ldapPort = properties.getOrDefault("ldap.port", 389).toString().toInt()
        val ldapContext = properties["ldap.context"].toString()
        val ldapUser = properties["ldap.user"].toString()
        val ldapPassword = properties["ldap.password"].toString()

        primaryProvider = LDAPProvider(ldapHost, ldapPort, ldapContext, ldapUser, ldapPassword)
    }

    private fun startCRON() = fixedRateTimer(javaClass.name, true, 0, (300 * 1000)) { sync() }

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
                val admin = u.role == Auth.BasicRoles.ADMIN.name
                val match = User.find { Users.username eq u.username and Users.from.eq(LDAPProvider.SOURCE_NAME) }
                if (match.empty()) {
                    // User doesn't exist yet
                    User.new {
                        username = u.username
                        hash = ""
                        token = UUID.randomUUID()
                        role = if (admin) auth.getDAOAdminRole() else auth.getDAOUserRole()
                        from = u.from
                    }
                } else match.elementAt(0).apply {
                    role = if (admin) auth.getDAOAdminRole() else auth.getDAOUserRole()
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
            invalid.forEach {
                it.delete()
            }
            if (invalid.size > 0) Log.w(javaClass, "Removed ${invalid.size} stale users")
        }
    }
}