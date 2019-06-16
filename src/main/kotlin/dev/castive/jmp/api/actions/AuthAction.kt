package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.AuthenticateResponse
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.Factor
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.ValidateRequest
import dev.castive.jmp.api.App
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.cache.HazelcastCacheLayer
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.util.SystemUtil
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.Context
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import javax.servlet.http.Cookie

object AuthAction {
	val cacheLayer = HazelcastCacheLayer()

	/**
	 * Create a new session
	 * This has no effect on external providers and is only user for tracking within JMP
	 * It is triggered by a users direct login or via an external provider (e.g. Crowds SSO cookie)
	 */
	fun createSession(user: User, token: String): Oauth.TokenResponse = transaction {
		val requestToken = TokenProvider.createRequestToken(user.username, token, user.role.name) ?: throw BadRequestResponse()
		val refreshToken = TokenProvider.createRefreshToken(user.username, token, user.role.name) ?: throw BadRequestResponse()
		Session.new {
			this.refreshToken = refreshToken
			this.ssoToken = token
			this.createdAt = System.currentTimeMillis()
			this.user = user
			this.active = true
		}
		Log.i(javaClass, "Creating session for ${user.username}")
		return@transaction Oauth.TokenResponse(requestToken, refreshToken)
	}
	fun onUserInvalid(token: String) {
		if(!cacheLayer.connected()) return
		Log.i(javaClass, "Removing cached user with token: $token")
		cacheLayer.removeUser(token)
	}
	fun onUserValid(user: User, token: String?) {
		if(!cacheLayer.connected()) return
		Log.i(javaClass, "Updating cached user with token: $token")
		cacheLayer.setUser(user.id.value, token ?: user.id.value.toString())
	}
	fun getAppId(): String? {
		return cacheLayer.getMisc("appId")
	}

	/**
	 * Check if the primary provider thinks that the token is valid
	 * Currently only support Atlassian Crowd
	 */
	fun isValidToken(token: String, ctx: Context): String {
		val valid = Providers.primaryProvider?.validate(token, ValidateRequest(arrayOf(Factor("remote_address", ctx.ip())))) ?: ""
		Log.d(javaClass, "Validation request returned: '$valid'")
		return valid
	}
	fun getTokenInfo(token: String, ctx: Context): AuthenticateResponse? {
		return kotlin.runCatching {
			SystemUtil.gson.fromJson(isValidToken(token, ctx), AuthenticateResponse::class.java)
		}.getOrNull()
	}
	fun writeInvalidCookie(ctx: Context, username: String? = null) {
		if(App.crowdCookieConfig != null) {
			// Set an invalid cookie
			val ck = Cookie(App.crowdCookieConfig!!.name, "").apply {
				this.domain = App.crowdCookieConfig!!.domain
				this.secure = App.crowdCookieConfig!!.secure
				this.path = "/"
				this.maxAge = 0
				this.comment = "This should be deleted!"
			}
			Log.a(javaClass, "Setting invalid SSO cookie for ${username ?: "[not given]"}")
			ctx.cookie(ck)
		}
	}

	/**
	 * Check if a user HAD a specific token at any point
	 */
	fun userHadToken(username: String?, token: String?): Session? {
		if(token == null) {
			Log.v(javaClass, "::userHadToken: provided null token")
			return null
		}
		return transaction {
			return@transaction Session.find {
				Sessions.ssoToken eq token
			}.elementAtOrNull(0)
		}
	}

	/**
	 * Check if a user has an active session with a specific token
	 */
	fun userHasToken(username: String?, token: String?): Session? {
		if(token == null) {
			Log.v(javaClass, "::userHasToken: provided null token")
			return null
		}
		return transaction {
			return@transaction Session.find {
				Sessions.ssoToken eq token and Sessions.active.eq(true)
			}.elementAtOrNull(0)
		}
	}
}