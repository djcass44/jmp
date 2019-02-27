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

package com.django.jmp.api

import com.amdelamar.jhash.Hash
import com.django.jmp.db.User
import com.django.jmp.db.Users
import io.javalin.ConflictResponse
import io.javalin.security.Role
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Auth {
    class BasicAuth(val username: String, val password: CharArray) {
        constructor(insecure: Insecure): this(insecure.username, insecure.password.toCharArray())
        class Insecure(val username: String, val password: String)
    }
    enum class BasicRoles: Role {
        USER, ADMIN
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
        return hashMatches(password.toCharArray(), expectedHash)
    }
    private fun hashMatches(password: CharArray, expectedHash: String): Boolean {
        return Hash.password(password).verify(expectedHash)
    }

    fun createUser(username: String, password: CharArray) {
        val hash = computeHash(password)
        if(!userExists(username)) { // Assume the user hasn't been added
            transaction {
                User.new {
                    this.username = username
                    this.hash = hash
                    this.token = UUID.randomUUID() // Generate an initial token
                }
            }
        }
        else
            throw ConflictResponse()
    }
    fun getUserToken(username: String, password: CharArray): String? {
        assert(userExists(username))
        return transaction {
            val existing = User.find {
                Users.username.lowerCase() eq username.toLowerCase()
            }
            val user = existing.elementAtOrNull(0)
            // Only return if hashes match (and user was found)
            return@transaction if(user != null && hashMatches(password, user.hash))
                user.token.toString()
            else
                null
        }
    }
    // This is an action performed by the system.
    fun putUserToken(username: String) {
        assert(userExists(username))
        transaction {
            val existing = User.find {
                Users.username.lowerCase() eq username.toLowerCase()
            }
            val user = existing.elementAtOrNull(0)
            user?.token = UUID.randomUUID() // Chance of collision is extremely low
        }
    }

    private fun userExists(username: String): Boolean {
        return transaction {
            val existing = User.find {
                Users.username.lowerCase() eq username.toLowerCase()
            }
            return@transaction !existing.empty()
        }
    }

    // Determine role based on request
    fun getUserRole(): Role {
        return BasicRoles.ADMIN // TODO DO NOT ALLOW IN PRODUCTION
    }
}