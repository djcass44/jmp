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
import dev.castive.javalin_auth.auth.provider.InternalProvider
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
			val login = if(basicAuth != null) auth.loginUser(basicAuth.username, basicAuth.password, ctx) else auth.loginUser(authHeader!!, ctx)
			if(login == null) {
				Log.d(javaClass, "Request attempt failed: notfound")
				throw NotFoundResponse()
			}
			val sso: AuthenticateResponse?
			val cookie: CrowdCookie?
			val name: String
			when {
				// Check with Crowd ONLY if the user is from Crowd
				Providers.primaryProvider is CrowdProvider && login.provided -> {
					sso = SystemUtil.gson.fromJson(login.token, AuthenticateResponse::class.java)
					// Try to generate the cookie if we can
					cookie = kotlin.runCatching { CrowdCookie(App.crowdCookieConfig!!.domain,
						"TRUE",
						App.crowdCookieConfig!!.secure,
						"",
						App.crowdCookieConfig!!.name,
						sso!!.token
					) }.getOrNull()
					name = sso.user.name
				}
				else -> {
					sso = null
					cookie = null
					name = basicAuth!!.username
				}
			}
			if(cookie != null) {
				// Set the cookie on the client
				val ck = Cookie(cookie.name, cookie.token).apply {
					this.domain = cookie.host
					this.secure = cookie.secure
					this.path = "/"
				}
				Log.d(javaClass, "Setting cookie to: $ck")
				ctx.cookie(ck)
			}
			val user = auth.getUser(name) ?: throw UnauthorizedResponse()
			val actualToken = sso?.token ?: login.token
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
				ClaimConverter.get(UserAction.getOrNull(ctx, lax = true), ctx)
			}.getOrNull() ?: throw UnauthorizedResponse()
			val response = transaction {
				val existingSession = Session.find { Sessions.user eq user.id and(Sessions.refreshToken eq refresh) }.limit(1).elementAtOrNull(0) ?: run {
					Log.d(Oauth::class.java, "Failed to find matching refresh token")
					throw UnauthorizedResponse("Invalid refresh token")
				}
				// Check if the users request token matched expected
				if(TokenProvider.verify(existingSession.refreshToken, verify) == null) throw BadRequestResponse("Expired refresh token")
				existingSession.active = false
				val result = AuthAction.createSession(user, existingSession.ssoToken ?: user.id.value.toString())
				Log.i(Oauth::class.java, "Refreshing session for ${user.username}")
				return@transaction result
			}
			ctx.status(HttpStatus.OK_200).json(response)
		}, Auth.openAccessRole)
		// Verify a users token is still valid
		get("${Runner.BASE}/v2/oauth/valid", { ctx ->
			Log.d(javaClass, "Checking session for ${ctx.ip()}")
			val user = ClaimConverter.getUser(ctx) ?: run {
//				AuthAction.writeInvalidCookie(ctx)
				throw UnauthorizedResponse("Invalid authentication")
			}
			val token = ClaimConverter.getToken(ctx)
			Log.d(javaClass, "Session for ${user.username} is valid")
			transaction {
				if(token != null && Providers.primaryProvider != null && user.from != InternalProvider.SOURCE_NAME) {
					if(AuthAction.isValidToken(token, ctx).isEmpty()) {
						Log.e(Oauth::class.java, "Primary provider validation failed")
//						AuthAction.writeInvalidCookie(ctx, user.username)
						throw UnauthorizedResponse("Invalid SSO token")
					}
					else if(App.crowdCookieConfig != null) {
						// Re-apply the cookie
						val ck = Cookie(App.crowdCookieConfig!!.name, token).apply {
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
			val ssoToken = ClaimConverter.getToken(ctx)
			transaction {
				val session = AuthAction.userHasToken(user.username, ssoToken)
				if(session?.ssoToken != null) {
					Providers.primaryProvider?.invalidateLogin(session.ssoToken!!)
					session.active = false
					Log.i(Oauth::class.java, "Invalidating login for ${user.username}, $ssoToken")
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