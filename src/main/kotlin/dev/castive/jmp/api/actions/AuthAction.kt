package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.Factor
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.ValidateRequest
import dev.castive.jmp.api.App
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.User
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.Context
import org.jetbrains.exposed.sql.transactions.transaction
import javax.servlet.http.Cookie

object AuthAction {
	fun createSession(user: User, token: String): Oauth.TokenResponse = transaction {
		val requestToken = TokenProvider.createRequestToken(user.username, token, user.role.name) ?: throw BadRequestResponse()
		val refreshToken = TokenProvider.createRefreshToken(user.username, token, user.role.name) ?: throw BadRequestResponse()
		Session.new {
			this.refreshToken = refreshToken
			this.createdAt = System.currentTimeMillis()
			this.user = user
		}
		Log.i(javaClass, "Creating session for ${user.username}")
		return@transaction Oauth.TokenResponse(requestToken, refreshToken)
	}
	fun isValidToken(token: String, ctx: Context): String {
		return Providers.primaryProvider?.validate(token, ValidateRequest(arrayOf(Factor("remote_address", ctx.ip())))) ?: ""
	}
	fun writeInvalidCookie(ctx: Context, username: String? = null) {
		if(App.crowdCookieConfig != null) {
			// Set an invalid cookie
			val ck = Cookie(App.crowdCookieConfig!!.name, "NO_CONTENT").apply {
				this.domain = App.crowdCookieConfig!!.domain
				this.secure = App.crowdCookieConfig!!.secure
				this.path = "/"
				this.isHttpOnly = false
			}
			Log.a(Oauth::class.java, "Setting invalid SSO cookie for ${username ?: "[not given]"}")
			ctx.cookie(ck)
		}
	}
}