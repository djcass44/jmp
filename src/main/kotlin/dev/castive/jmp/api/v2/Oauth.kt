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
import dev.castive.javalin_auth.auth.provider.CrowdProvider
import dev.castive.jmp.Runner
import dev.castive.jmp.api.App
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.auth.UserVerification
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.UserData
import dev.castive.jmp.util.SystemUtil
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import javax.servlet.http.Cookie

class Oauth(private val auth: Auth, private val verify: UserVerification): EndpointGroup {
	data class TokenResponse(val request: String, val refresh: String)
	override fun addEndpoints() {
		get("${Runner.BASE}/v2/oauth/cookie", { ctx ->
			if(App.crowdCookieConfig == null) {
				ctx.status(HttpStatus.NOT_FOUND_404).result("No SSO config has been loaded")
				return@get
			}
			val ck = ctx.cookie(App.crowdCookieConfig!!.name)
			Log.d(javaClass, "Users HTTP cookie: $ck, valid: ${ck != null && ck != "NO_CONTENT"}")
			ctx.status(HttpStatus.OK_200).json(ck != null && ck != "NO_CONTENT")
		}, Auth.openAccessRole)
		// Get a users token
		post("${Runner.BASE}/v2/oauth/token", { ctx ->
			val basicAuth = ctx.basicAuthCredentials()
			val authHeader = if(App.crowdCookieConfig != null) ctx.cookie(App.crowdCookieConfig!!.name) else null
			if(basicAuth == null && authHeader == null) {
				Log.e(javaClass, "Not given any form of identification, cannot authenticate user")
				throw BadRequestResponse("No auth form given")
			}
			val token = if(basicAuth != null) auth.loginUser(basicAuth.username, basicAuth.password, ctx) else auth.loginUser(authHeader!!, ctx)
			if(token == null) {
				Log.d(javaClass, "Request attempt failed: notfound")
				throw NotFoundResponse()
			}
			val sso: AuthenticateResponse?
			val cookie: CrowdCookie?
			val name: String
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
					name = sso.user.name
				}
				else -> {
					sso = null
					cookie = null
					name = basicAuth!!.username
				}
			}
			if(cookie != null) {
				val ck = Cookie(cookie.name, cookie.token).apply {
					this.domain = cookie.host
					this.secure = cookie.secure
					this.path = "/"
				}
				Log.d(javaClass, "Setting cookie to: $ck")
				ctx.cookie(ck)
			}
			val user = auth.getUser(name) ?: throw UnauthorizedResponse()
			val actualToken = sso?.token ?: token
			transaction {
				user.requestToken = actualToken
			}
			val result = AuthAction.createSession(user, actualToken)
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
				ClaimConverter.get(UserAction.get(ctx, lax = true), ctx)
			}.getOrNull() ?: throw UnauthorizedResponse()
			val response = transaction {
				val existingRefreshToken = Session.find { Sessions.user eq user.id and(Sessions.refreshToken eq refresh) }.limit(1).elementAtOrNull(0) ?: run {
					Log.d(Oauth::class.java, "Failed to find matching refresh token")
					throw UnauthorizedResponse("Invalid refresh token")
				}
				// Check if the users request token matched expected
				if(TokenProvider.verify(existingRefreshToken.refreshToken, verify) == null) throw BadRequestResponse("Expired refresh token")
				val result = AuthAction.createSession(user, user.requestToken!!)
				Log.i(Oauth::class.java, "Refreshing session for ${user.username}")
				return@transaction result
			}
			ctx.status(HttpStatus.OK_200).json(response)
		}, Auth.openAccessRole)
		// Verify a users token is still valid
		get("${Runner.BASE}/v2/oauth/valid", { ctx ->
			Log.d(javaClass, "Checking session for ${ctx.host()}")
			val user = ClaimConverter.getUser(ctx) ?: run {
				AuthAction.writeInvalidCookie(ctx)
				throw UnauthorizedResponse("Invalid authentication")
			}
			Log.d(javaClass, "Session for ${user.username} is valid")
			transaction {
				if(user.requestToken != null && Providers.primaryProvider != null) {
					if(AuthAction.isValidToken(user.requestToken!!, ctx).isEmpty()) {
						Log.e(Oauth::class.java, "Primary provider validation failed")
						AuthAction.writeInvalidCookie(ctx, user.username)
						throw UnauthorizedResponse("Invalid SSO token")
					}
					else if(App.crowdCookieConfig != null) {
						// Re-apply the cookie
						val ck = Cookie(App.crowdCookieConfig!!.name, user.requestToken).apply {
							this.domain = App.crowdCookieConfig!!.domain
							this.secure = App.crowdCookieConfig!!.secure
							this.path = "/"
						}
						Log.v(Oauth::class.java, "Setting SSO cookie for ${user.username}")
						ctx.cookie(ck)
					}
				}
				ctx.status(HttpStatus.OK_200).json(UserData(user))
			}
		}, Auth.openAccessRole)
		// Logout the user and invalidate tokens if needed
		post("${Runner.BASE}/v2/oauth/logout", { ctx ->
			val user = ClaimConverter.get(ctx)
			transaction {
				if(user.requestToken != null) {
					Providers.primaryProvider?.invalidateLogin(user.requestToken!!)
					Log.i(Oauth::class.java, "Invalidating login for ${user.username}, ${user.requestToken}")
				}
				else Log.v(javaClass, "User has no token to invalidate: ${user.username}")
			}
			ctx.status(HttpStatus.OK_200)
		}, Auth.defaultRoleAccess)
	}
}

private fun String?.isJSNullOrBlank(): Boolean {
	return this.isNullOrBlank() || this == "null" || this == "undefined"
}