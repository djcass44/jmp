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
import dev.castive.javalin_auth.auth.data.User
import dev.castive.javalin_auth.auth.external.UserIngress
import dev.castive.javalin_auth.auth.provider.LDAPProvider
import dev.castive.jmp.api.Auth
import dev.castive.jmp.db.dao.Users
import dev.castive.log2.Log
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class UserValidator(private val auth: Auth, private val ldapConfigExtras: LDAPConfig.Extras): UserIngress {
    override fun ingestUsers(users: ArrayList<User>) {
        val names = arrayListOf<String>()
        transaction {
            users.forEach { u ->
                names.add(u.username)
                val match = dev.castive.jmp.db.dao.User.find { Users.username eq u.username and Users.from.eq(LDAPProvider.SOURCE_NAME) }
                if(match.empty()) {
                    // User doesn't exist yet
                    dev.castive.jmp.db.dao.User.new {
                        username = u.username
                        hash = ""
                        role = auth.getDAOUserRole()
                        from = u.source
                    }
                }
                else match.elementAt(0).apply {
                    // Update user details if needed
                }
            }
            // Get LDAP users which weren't in the most recent search and delete them
            val externalUsers = dev.castive.jmp.db.dao.User.find { Users.from eq LDAPProvider.SOURCE_NAME }
            val invalid = arrayListOf<dev.castive.jmp.db.dao.User>()
            externalUsers.forEach { if(!names.contains(it.username)) invalid.add(it) }
            Log.i(javaClass, "Found ${invalid.size} stale users")
            if(ldapConfigExtras.removeStale) {
                invalid.forEach { it.delete() }
                if(invalid.size > 0) Log.w(javaClass, "Removed ${invalid.size} stale users")
            }
            else Log.i(javaClass, "Stale user removal blocked by application policy")
        }
    }
}