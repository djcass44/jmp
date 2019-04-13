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

package dev.castive.jmp.auth.provider

import dev.castive.log2.Log
import dev.castive.jmp.auth.connect.LDAPConnection
import dev.castive.jmp.db.dao.GroupData
import dev.castive.jmp.db.dao.UserData
import java.util.*
import javax.naming.AuthenticationException
import javax.naming.NamingException

class LDAPProvider(private val server: String,
                   private val port: Int = 389,
                   private val contextDN: String,
                   private val serviceUserDN: String,
                   private val serviceUserPassword: String,
                   private val filter: String,
                   private val identifier: String): BaseProvider {
    companion object {
        const val SOURCE_NAME = "ldap"
    }
    private val auth = dev.castive.jmp.api.Auth()
    private lateinit var connection: LDAPConnection

    var connected = false
        private set

    override fun setup() = try {
        connection = LDAPConnection(server, port, contextDN, serviceUserDN, serviceUserPassword)
        connected = connection.connected
        Log.i(javaClass, "LDAP connected: $connected")
    }
    catch (e: AuthenticationException) {
        connected = false
        Log.e(javaClass, "LDAP -> Wrong authentication")
    }
    catch (e: NamingException) {
        connected = false
        Log.e(javaClass, "LDAP -> Couldn't connect: $e")
    }

    override fun getUsers(): ArrayList<UserData>? {
        val users = arrayListOf<UserData>()
        val result = connection.searchFilter(filter) ?: return null
        for (r in result) {
            val username = r.attributes.get(identifier).get(0).toString()
            val role = r.attributes.get("objectClass").get(0).toString()
            users.add(UserData(UUID.randomUUID(), username, role, arrayListOf(), 0, 0, SOURCE_NAME))
        }
        return users
    }
    override fun getGroups(): ArrayList<GroupData> {
        throw NotImplementedError()
    }
    override fun tearDown() {
        connection.close()
    }

    override fun getLogin(uid: String, password: String): String? {
        val valid = connection.checkUserAuth(uid, password, identifier)
        return if (valid) auth.getUserTokenWithPrivilege(uid)
        else null
    }

    override fun getName(): String {
        return SOURCE_NAME
    }

    override fun connected(): Boolean {
        return connection.connected
    }
}