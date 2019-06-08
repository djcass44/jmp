package dev.castive.jmp.api.actions

import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.User
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import org.jetbrains.exposed.sql.transactions.transaction

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
}