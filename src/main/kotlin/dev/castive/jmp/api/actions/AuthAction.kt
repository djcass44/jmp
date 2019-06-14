package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.Factor
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.ValidateRequest
import dev.castive.jmp.api.App
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.Context
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import javax.servlet.http.Cookie

object AuthAction {
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
	fun isValidToken(token: String, ctx: Context): String {
		val valid = Providers.primaryProvider?.validate(token, ValidateRequest(arrayOf(Factor("remote_address", ctx.ip())))) ?: ""
		Log.d(javaClass, "Validation request returned: '$valid'")
		return valid
	}
	fun writeInvalidCookie(ctx: Context, username: String? = null) {
		if(App.crowdCookieConfig != null) {
			// Set an invalid cookie
			val ck = Cookie(App.crowdCookieConfig!!.name, "NO_CONTENT").apply {
				this.domain = App.crowdCookieConfig!!.domain
				this.secure = App.crowdCookieConfig!!.secure
				this.path = "/"
			}
			Log.a(javaClass, "Setting invalid SSO cookie for ${username ?: "[not given]"}")
			ctx.cookie(ck)
		}
	}
	fun userHadToken(username: String?, token: String?): Session? {
		if(username == null) {
			Log.v(javaClass, "::userHadToken: provided null username")
			return null
		}
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
	fun userHasToken(username: String?, token: String?): Session? {
		if(username == null) {
			Log.v(javaClass, "::userHasToken: provided null username")
			return null
		}
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