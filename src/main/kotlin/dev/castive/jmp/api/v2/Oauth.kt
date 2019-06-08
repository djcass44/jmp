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

import dev.castive.javalin_auth.actions.UserAction
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.AuthenticateResponse
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.CrowdCookie
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.Factor
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.ValidateRequest
import dev.castive.javalin_auth.auth.provider.CrowdProvider
import dev.castive.jmp.Runner
import dev.castive.jmp.api.App
import dev.castive.jmp.api.Auth
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.auth.UserVerification
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.UserData
import dev.castive.jmp.util.SystemUtil
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.InternalServerErrorResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.servlet.http.Cookie

class Oauth(private val auth: Auth, private val verify: UserVerification): EndpointGroup {
	data class TokenResponse(val request: String, val refresh: String)
	data class SSOResponse(val request: String, val refresh: String, val sso: Any?)
	override fun addEndpoints() {
		// Get a users token
		post("${Runner.BASE}/v2/oauth/token", { ctx ->
			val basicAuth = ctx.basicAuthCredentials() ?: run {
				Log.d(javaClass, "Oauth request attempt failed: invalid basic auth")
				throw BadRequestResponse()
			}
			val token = auth.loginUser(basicAuth.username, basicAuth.password) ?: run {
				Log.d(javaClass, "${basicAuth.username} request attempt failed: notfound")
				throw NotFoundResponse()
			}
			val sso: AuthenticateResponse?
			val cookie: CrowdCookie?
			when(Providers.primaryProvider) {
				is CrowdProvider -> {
					sso = SystemUtil.gson.fromJson(token, AuthenticateResponse::class.java)
					cookie = CrowdCookie(App.crowdCookieConfig!!.domain,
						"TRUE",
						App.crowdCookieConfig!!.secure,
						"",
						App.crowdCookieConfig!!.name,
						sso!!.token
					)
				}
				else -> {
					sso = null
					cookie = null
				}
			}
			if(cookie != null) {
				val ck = Cookie(cookie.name, URLEncoder.encode("token=${cookie.token};", StandardCharsets.UTF_8)).apply {
					this.domain = cookie.host
					this.secure = cookie.secure
					this.path = "/"
				}
				Log.d(javaClass, "Setting cookie to: $ck")
				ctx.cookie(ck)
			}
			val user = auth.getUser(basicAuth.username) ?: throw UnauthorizedResponse()
			val actualToken = sso?.token ?: token
			transaction {
				user.requestToken = actualToken
			}
			val result = transaction {
				val requestToken = TokenProvider.createRequestToken(basicAuth.username, actualToken, user.role.name) ?: throw BadRequestResponse()
				val refreshToken = TokenProvider.createRefreshToken(basicAuth.username, actualToken, user.role.name) ?: throw BadRequestResponse()
				Session.new {
					this.refreshToken = refreshToken
					this.createdAt = System.currentTimeMillis()
					this.user = user
				}
				Log.i(javaClass, "Creating session for ${user.username}")
				return@transaction SSOResponse(requestToken, refreshToken, sso)
			}
			ctx.status(HttpStatus.OK_200).json(result)
		}, Auth.openAccessRole)
		get("${Runner.BASE}/v2/oauth/refresh", { ctx ->
			val refresh = ctx.queryParam("refreshToken", "")
			if(refresh.isJSNullOrBlank()) {
				Log.d(javaClass, "Refresh token is null or blank")
				throw BadRequestResponse()
			}
			Log.d(javaClass, "Found refresh token")
			refresh!!
			val user = kotlin.runCatching {
				ClaimConverter.get(UserAction.get(ctx, lax = true))
			}.getOrNull() ?: throw UnauthorizedResponse()
			val response = transaction {
				val existingRefreshToken = Session.find { Sessions.user eq user.id and(Sessions.refreshToken eq refresh) }.limit(1).elementAtOrNull(0) ?: run {
					Log.d(Oauth::class.java, "Failed to find matching refresh token")
					throw UnauthorizedResponse("Invalid refresh token")
				}
				// Check if the users request token matched expected
				if(TokenProvider.verify(existingRefreshToken.refreshToken, verify) == null) throw BadRequestResponse("Expired refresh token")
				val requestToken = TokenProvider.createRequestToken(user.username, user.requestToken ?: "", user.role.name) ?: throw InternalServerErrorResponse()
				val refreshToken = TokenProvider.createRefreshToken(user.username, user.requestToken ?: "", user.role.name) ?: throw InternalServerErrorResponse()
				Session.new {
					this.refreshToken = refreshToken
					this.createdAt = System.currentTimeMillis()
					this.user = user
				}
				Log.i(Oauth::class.java, "Refreshing session for ${user.username}")
				return@transaction TokenResponse(requestToken, refreshToken)
			}
			ctx.status(HttpStatus.OK_200).json(response)
		}, Auth.openAccessRole)
		// Verify a users token is still valid
		get("${Runner.BASE}/v2/oauth/valid", { ctx ->
			Log.d(javaClass, "Checking session for ${ctx.host()}")
			val claimedUser = ClaimConverter.getUser(UserAction.getOrNull(ctx))
			val user = (if(App.crowdCookieConfig != null) {
				// Check for Crowd SSO cookie
				val cookie = kotlin.runCatching {
					SystemUtil.gson.fromJson(ctx.cookie(App.crowdCookieConfig!!.name), CrowdCookie::class.java)
				}.getOrNull()
				if(cookie == null) {
					Log.e(javaClass, "Failed to deserialise CrowdCookie")
					claimedUser
				}
				else auth.getUserWithSSOToken(cookie.token) ?: claimedUser
			} else {
				throw UnauthorizedResponse("Invalid token")
			}) ?: throw UnauthorizedResponse("Not a valid user")
			Log.d(javaClass, "Session for ${user.username} is valid")
			transaction {
				if(user.requestToken != null) {
					if(Providers.primaryProvider?.validate(user.requestToken!!, ValidateRequest(arrayOf(Factor("remote_address", ctx.ip())))) == false) {
						Log.e(javaClass, "Primary provider validation failed")
						throw UnauthorizedResponse("Invalid SSO token")
					}
				}
				ctx.status(HttpStatus.OK_200).json(UserData(user))
			}
		}, Auth.openAccessRole)
	}
}

private fun String?.isJSNullOrBlank(): Boolean {
	return this.isNullOrBlank() || this == "null" || this == "undefined"
}