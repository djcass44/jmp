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

package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.JWT
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.response.AuthenticateResponse
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.db.dao.User
import dev.castive.log2.Log
import io.javalin.Context
import io.javalin.ForbiddenResponse

object UserAction {
    private val verification = Providers.verification

    internal fun get(ctx: Context): User {
        val jwt = JWT.get().map(ctx) ?: run {
            ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
            throw ForbiddenResponse("Token verification failed")
        }
        Log.ok(javaClass, "JWT parse valid")
        return ClaimConverter.getUser(TokenProvider.get().verify(jwt, verification)) ?: run {
            ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
            throw ForbiddenResponse("Token verification failed")
        }
    }
    internal fun getOrNull(ctx: Context): User? {
        val jwt = JWT.get().map(ctx) ?: ""
        return if(jwt == "null" || jwt.isBlank()) null else ClaimConverter.getUser(TokenProvider.get().verify(jwt, verification))
    }
}