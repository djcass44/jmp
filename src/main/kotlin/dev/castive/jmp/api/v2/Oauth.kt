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

package dev.castive.jmp.api.v2

import dev.castive.javalin_auth.auth.JWT
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.response.AuthenticateResponse
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.db.Util
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.ForbiddenResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class Oauth(private val auth: Auth): EndpointGroup {
    data class TokenResponse(val jwt: String, val request: String)
    override fun addEndpoints() {
        // Get a users token
        post("${Runner.BASE}/v2/oauth/token", { ctx ->
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            val token = auth.loginUser(basicAuth.username, basicAuth.password) ?: throw NotFoundResponse()
            val user = auth.getUser(basicAuth.username, Util.getSafeUUID(token)) ?: throw UnauthorizedResponse()
            val jwt = TokenProvider.get().createRequestToken(basicAuth.username, token) ?: throw BadRequestResponse()
            val refreshToken = TokenProvider.get().createRefreshToken(basicAuth.username, token) ?: throw BadRequestResponse()
            Log.ok(javaClass, "Generated refresh token: $refreshToken")
            transaction {
                Session.new {
                    this.refreshToken = refreshToken
                    this.createdAt = System.currentTimeMillis()
                    this.user = user
                }
                Log.i(javaClass, "Creating session for ${user.username}")
            }
            ctx.status(HttpStatus.OK_200).json(TokenResponse(jwt, refreshToken))
        }, Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2/oauth/refresh", { ctx ->
            val refresh = ctx.queryParam("refresh_token", "")
            if(refresh.isNullOrBlank()) {
                Log.d(javaClass, "Refresh token is null or blank")
                throw BadRequestResponse()
            }
            val jwt = JWT.get().map(ctx) ?: run {
                Log.d(javaClass, "Token verification failed")
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            if (!TokenProvider.get().mayBeToken(jwt)) throw BadRequestResponse()
            val user = ClaimConverter.getUser(TokenProvider.get().verifyLax(jwt, Providers.verification)) ?: throw BadRequestResponse()
            val response = transaction {
                val existingRefreshToken = Session.find {
                    Sessions.user eq user.id and(Sessions.refreshToken eq refresh)
                }.limit(1).elementAtOrNull(0) ?: run {
                    Log.d(Oauth::class.java, "Failed to find matching refresh token")
                    throw UnauthorizedResponse("Invalid refresh token")
                }
                // Check if users request token matched expected
                if(TokenProvider.get().verify(existingRefreshToken.refreshToken, Providers.verification) == null) {
                    throw BadRequestResponse("Expired refresh token")
                }
                val newToken = TokenProvider.get().createRequestToken(user.username, user.id.value.toString()) ?: throw BadRequestResponse()
                val refreshToken = TokenProvider.get().createRefreshToken(user.username, user.id.value.toString()) ?: throw BadRequestResponse()
                Session.new {
                    this.refreshToken = refreshToken
                    this.createdAt = System.currentTimeMillis()
                    this.user = user
                }
                Log.i(Oauth::class.java, "Refreshing session for ${user.username}")
                return@transaction TokenResponse(newToken, refreshToken)
            }
            ctx.status(HttpStatus.OK_200).json(response)
        }, Auth.defaultRoleAccess)
        // Verify a users token is still valid
        get("${Runner.BASE}/v2/oauth/valid", { ctx ->
            val jwt = JWT.get().map(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            if (!TokenProvider.get().mayBeToken(jwt)) throw BadRequestResponse()
            val user = ClaimConverter.getUser(TokenProvider.get().verify(jwt, Providers.verification)) ?: throw ForbiddenResponse()
            transaction {
                ctx.status(HttpStatus.OK_200).result(auth.validateUserToken(user.id.value).toString())
            }
        }, Auth.defaultRoleAccess)
    }
}