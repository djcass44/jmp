package dev.castive.jmp.auth

import dev.castive.javalin_auth.api.OAuth2
import dev.castive.javalin_auth.auth.provider.flow.AbstractOAuth2Provider
import dev.castive.jmp.api.App
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.existsByUsername
import dev.castive.jmp.db.repo.findFirstByRefreshTokenAndActive
import dev.castive.log2.Log
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class OAuth2Callback: OAuth2.Callback() {
	/**
	 * Create a user by getting their information from the provider
	 */
	override fun createUser(accessToken: String, refreshToken: String, provider: AbstractOAuth2Provider): Boolean {
		val userData = provider.getUserInformation(accessToken)
		if(userData == null) {
			Log.e(javaClass, "Failed to get user information for using with token: $accessToken")
			return false
		}
		// Only create the user if they don't exist
		if(!Users.existsByUsername(userData.username)) {
			// create the user
			val user = transaction {
				return@transaction User.new {
					username = userData.username
					displayName = userData.displayName
					avatarUrl = userData.avatarUrl
					hash = ""
					role = App.auth.getDAOUserRole() // assume user for now
					from = userData.source
				}
			}
			// Create a session for the new user
			newSession(refreshToken, user)
		}
		else {
			Log.i(javaClass, "User already exists: ${userData.username}")
			// Create/update the session for the existing user
			val user = transaction {
				val u = User.find { Users.username eq userData.username and(Users.from eq userData.source) }.limit(1).elementAtOrNull(0)
				// Update some things which may change
				u?.apply {
					displayName = userData.displayName
					avatarUrl = userData.avatarUrl
				}
			}
			newSession(refreshToken, user)
		}
		return true
	}

	/**
	 * Create and update the sessions for the user
	 * @param user: what user we want to create the session for. This can be null if there is an active session (e.g. for refreshing)
	 */
	private fun newSession(refreshToken: String, user: User?, oldToken: String = refreshToken) {
		val existingSession = Sessions.findFirstByRefreshTokenAndActive(oldToken)
		existingSession?.active = false

		if(user == null && existingSession == null) {
			Log.w(javaClass, "Unable to create session as we have no context of the user")
			return
		}
		// Create the new session
		transaction {
			Session.new {
				this.refreshToken = refreshToken
				// We MUST have one or the other
				this.user = user ?: existingSession!!.user
				ssoToken = null
				active = true
			}
		}
	}
}
