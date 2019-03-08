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

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.django.jmp.api.Auth
import com.django.jmp.db.User
import com.django.jmp.db.Users
import com.django.log2.logging.Log
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.TimeUnit

class TokenProvider {
    companion object {
        private lateinit var tokenProvider: TokenProvider
        fun getInstance(): TokenProvider {
            if(!this::tokenProvider.isInitialized)
                tokenProvider = TokenProvider()
            return tokenProvider
        }
    }
//    private val algorithm: Algorithm = Algorithm.HMAC256(PasswordGenerator.getInstance().getInsecure(32))
    private val algorithm: Algorithm = Algorithm.HMAC256("secret")

    fun create(user: String, userToken: String): String? = try {
        // Expires in 8 hours
        val expiry = Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(8))
        JWT.create()
            .withIssuer(javaClass.name)
            .withClaim(Auth.headerUser, user)
            .withClaim(Auth.headerToken, userToken)
            .withExpiresAt(expiry)
            .withIssuedAt(Date(System.currentTimeMillis()))
            .sign(algorithm)
    }
    catch (e: Exception) {
        Log.e(javaClass, "Failed to generate token: [user: $user, cause: $e]")
        null
    }
    fun verify(token: String): User? {
        val verify = JWT.require(algorithm)
            .withIssuer(javaClass.name)
            .build()
        return try {
            val result = verify.verify(token)
            if(result.expiresAt.before(Date(System.currentTimeMillis()))) // Token has expired
                return null
            transaction {
                User.find {
                    Users.username eq result.getClaim(Auth.headerUser).asString() and
                            Users.token.eq(UUID.fromString(result.getClaim(Auth.headerToken).asString()))
                }.limit(1).elementAtOrNull(0)
            }
        }
        catch (e: Exception) {
            Log.e(javaClass, "Failed token verification: $e")
            e.printStackTrace()
            null
        }
    }
}