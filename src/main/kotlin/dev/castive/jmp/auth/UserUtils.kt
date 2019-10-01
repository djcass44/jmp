package dev.castive.jmp.auth

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.AuthenticateResponse
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.Factor
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.ValidateRequest
import dev.castive.jmp.api.App
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.cache.BaseCacheLayer
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.util.parse
import dev.castive.log2.Log
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import javax.servlet.http.Cookie

class UserUtils(val cache: BaseCacheLayer) {
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
	fun getSession(user: User, refreshToken: String) = transaction {
		Session.find {
			// Find ACTIVE matching sessions
			Sessions.user eq user.id and (Sessions.refreshToken eq refreshToken) and (Sessions.active eq true)
		}.limit(1).elementAtOrNull(0)
	}

	fun getSession(refreshToken: String) = transaction {
		Session.find {
			Sessions.refreshToken eq refreshToken and (Sessions.active eq true)
		}.limit(1).elementAtOrNull(0)
	}

	fun onUserInvalid(token: String) {
		if(!cache.connected()) return
		Log.i(javaClass, "Removing cached user with token: $token")
		cache.removeUser(token)
	}
	fun onUserValid(user: User, token: String?) {
		if(!cache.connected()) return
		Log.i(javaClass, "Updating cached user with token: $token")
		cache.setUser(user.username, token ?: user.id.value.toString())
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
		return runCatching {
			isValidToken(token, ctx).parse(AuthenticateResponse::class.java)
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
	/**
	 * Check whether a user exists or not
	 */
	fun userExists(username: String): Boolean {
		return transaction {
			val existing = User.find {
				Users.username.lowerCase() eq username.toLowerCase()
			}
			return@transaction !existing.empty()
		}
	}

	/**
	 * Get a user by their username
	 */
	fun getUser(username: String): User? {
		return transaction {
			return@transaction User.find {
				Users.username eq username
			}.elementAtOrNull(0)
		}
	}
}