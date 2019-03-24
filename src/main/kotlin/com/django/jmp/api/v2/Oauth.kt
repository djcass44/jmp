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

package com.django.jmp.api.v2

import com.django.jmp.api.Auth
import com.django.jmp.api.Runner
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.auth.response.AuthenticateResponse
import io.javalin.BadRequestResponse
import io.javalin.ForbiddenResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Oauth(private val auth: Auth): EndpointGroup {
    data class TokenResponse(val jwt: String, val request: String)
    override fun addEndpoints() {
        // Get a users token
        post("${Runner.BASE}/v2/oauth/token", { ctx ->
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            val token = auth.loginUser(basicAuth.username, basicAuth.password) ?: throw NotFoundResponse()
            val user = auth.getUser(basicAuth.username, UUID.fromString(token)) ?: throw BadRequestResponse()
            val jwt = TokenProvider.getInstance().create(basicAuth.username, token) ?: throw BadRequestResponse()
            val newRequestToken = TokenProvider.getInstance().create(basicAuth.username) ?: throw BadRequestResponse()
            transaction {
                user.requestToken = newRequestToken
            }
            ctx.status(HttpStatus.OK_200).json(TokenResponse(jwt, newRequestToken))
        }, Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2/oauth/refresh", { ctx ->
            val refresh = ctx.queryParam("refresh_token", "")
            if(refresh.isNullOrBlank()) throw BadRequestResponse()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            if (!TokenProvider.getInstance().mayBeToken(jwt)) throw BadRequestResponse()
            val user = TokenProvider.getInstance().decode(jwt) ?: throw BadRequestResponse()
            // Check if users request token matched expected
            if(user.requestToken != refresh) throw UnauthorizedResponse("Invalid refresh token")
            val newToken = TokenProvider.getInstance().create(user.username, user.token.toString()) ?: throw BadRequestResponse()
            val newRequestToken = TokenProvider.getInstance().create(user.username) ?: throw BadRequestResponse()
            transaction {
                user.requestToken = newRequestToken
            }
            ctx.status(HttpStatus.OK_200).json(TokenResponse(newToken, newRequestToken))
        }, Auth.defaultRoleAccess)
        // Verify a users token is still valid
        get("${Runner.BASE}/v2/oauth/valid", { ctx ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            if (!TokenProvider.getInstance().mayBeToken(jwt)) throw BadRequestResponse()
            val user = TokenProvider.getInstance().verify(jwt) ?: throw ForbiddenResponse()
            transaction {
                ctx.status(HttpStatus.OK_200).result(auth.validateUserToken(user.token).toString())
            }
        }, Auth.defaultRoleAccess)
    }
}