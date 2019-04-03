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

package dev.castive.jmp.api

import com.amdelamar.jhash.Hash
import com.django.log2.logging.Log
import dev.castive.jmp.auth.Providers
import dev.castive.jmp.db.dao.Roles
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import io.javalin.ConflictResponse
import io.javalin.security.Role
import io.javalin.security.SecurityUtil
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Auth {
    @Deprecated(message = "Use 'Authorization' headers instead", level = DeprecationLevel.ERROR)
    class BasicAuth(val username: String, val password: CharArray) {
        constructor(insecure: Insecure): this(insecure.username, insecure.password.toCharArray())
        class Insecure(val username: String, val password: String)
    }
    enum class BasicRoles: Role {
        USER, ADMIN
    }
    companion object {
        const val headerToken = "X-Auth-Token"
        const val headerUser = "X-Auth-User"

        val defaultRoleAccess = SecurityUtil.roles(
            Auth.BasicRoles.USER,
            Auth.BasicRoles.ADMIN
        )
        val adminRoleAccess = SecurityUtil.roles(Auth.BasicRoles.ADMIN)
    }

    @Deprecated(message = "Do not use strings when dealing with passwords.")
    fun computeHash(password: String): String {
        return computeHash(password.toCharArray())
    }
    fun computeHash(password: CharArray): String {
        return Hash.password(password).create()
    }
    @Deprecated(message = "Do not use strings when dealing with passwords.")
    fun hashMatches(password: String, expectedHash: String): Boolean {
        if(expectedHash.isBlank()) return false
        return hashMatches(password.toCharArray(), expectedHash)
    }
    private fun hashMatches(password: CharArray, expectedHash: String): Boolean {
        if(expectedHash.isBlank()) return false
        return Hash.password(password).verify(expectedHash)
    }

    fun createUser(username: String, password: CharArray, admin: Boolean = false) {
        val hash = computeHash(password)
        if(!userExists(username)) { // Assume the user hasn't been added
            transaction {
                User.new {
                    this.username = username
                    this.hash = hash
                    role = if(admin) getDAOAdminRole() else getDAOUserRole()
                    metaCreation = System.currentTimeMillis()
                    metaUpdate = System.currentTimeMillis()
                }
            }
        }
        else
            throw ConflictResponse()
    }
    fun loginUser(username: String, password: String): String? {
        var result: String?
        if(Providers.primaryProvider != null) { // Try to use primary provider if it exists
            result = Providers.primaryProvider?.getLogin(username, password)
            if(result != null) return result
        }
        Log.v(javaClass, "Failed to locate user in primary provider, checking local provider")
        // Fallback to internal database checks
        result = Providers.internalProvider.getLogin(username, password)
        Log.d(javaClass, "Found local user: $result")
        return result
    }

    fun validateUserToken(token: UUID): Boolean {
        return transaction {
            val existing = User.findById(token)
            return@transaction existing != null
        }
    }

    fun getUserToken(username: String, password: CharArray): String? {
        assert(userExists(username))
        return transaction {
            val existing = User.find {
                Users.username eq username
            }.limit(1)
            val user = existing.elementAtOrNull(0)
            // Only return if hashes match (and user was found)
            return@transaction if(user != null && hashMatches(password, user.hash))
                user.id.value.toString()
            else
                null
        }
    }

    /**
     * Get a users token without checking password
     * ONLY USE THIS ONCE EXTERNAL AUTHENTICATION HAS SUCCEEDED
     */
    fun getUserTokenWithPrivilege(username: String): String {
        assert(userExists(username)) // Requires users to be synced already
        return transaction {
            val existing = User.find {
                Users.username eq username
            }.limit(1)
            val user = existing.elementAtOrNull(0)
            return@transaction user?.id?.value.toString()
        }
    }
    fun userExists(username: String): Boolean {
        return transaction {
            val existing = User.find {
                Users.username.lowerCase() eq username.toLowerCase()
            }
            return@transaction !existing.empty()
        }
    }

    private fun getUser(username: String): User? {
        return transaction {
            return@transaction User.find {
                Users.username eq username
            }.elementAtOrNull(0)
        }
    }
    fun getUser(username: String?, token: UUID?): User? {
        if(username == null || token == null)
            return null
        return transaction {
            return@transaction User.findById(token)
        }
    }

    // Determine role based on request
    fun getUserRole(username: String? = null): Role {
        if(username == null)
            return Auth.BasicRoles.USER
        val user = getUser(username) ?: return Auth.BasicRoles.USER
        return transaction {
            when (user.role.name) {
                Auth.BasicRoles.ADMIN.name -> Auth.BasicRoles.ADMIN
                Auth.BasicRoles.USER.name -> Auth.BasicRoles.USER
                else -> Auth.BasicRoles.USER
            }
        }
    }
    // Determine role based on request
    fun getUserRole(username: String, token: UUID): Role {
        val user = getUser(username) ?: return Auth.BasicRoles.USER
        if(user.id.value != token) {
            Log.w(javaClass, "getUserRole -> $user provided invalid token")
            return Auth.BasicRoles.USER
        }
        return transaction {
            when (user.role.name) {
                Auth.BasicRoles.ADMIN.name -> Auth.BasicRoles.ADMIN
                Auth.BasicRoles.USER.name -> Auth.BasicRoles.USER
                else -> Auth.BasicRoles.USER
            }
        }
    }
    fun getDAOUserRole(): dev.castive.jmp.db.dao.Role {
        return getDAORole(Auth.BasicRoles.USER)
    }
    fun getDAOAdminRole(): dev.castive.jmp.db.dao.Role {
        return getDAORole(Auth.BasicRoles.ADMIN)
    }
    private fun getDAORole(role: Auth.BasicRoles): dev.castive.jmp.db.dao.Role {
        return transaction {
            return@transaction dev.castive.jmp.db.dao.Role.find {
                Roles.name eq role.name
            }.elementAtOrNull(0)?: // If role is null, create it
            dev.castive.jmp.db.dao.Role.new {
                name = role.name
            }
        }
    }
}