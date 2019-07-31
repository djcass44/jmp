package dev.castive.jmp.api.v2_1

import com.github.scribejava.core.model.OAuth2AccessToken
import dev.castive.javalin_auth.auth.provider.GitHubProvider
import dev.castive.javalin_auth.auth.provider.GoogleProvider
import dev.castive.javalin_auth.auth.provider.flow.AbstractOAuth2Provider
import dev.castive.javalin_auth.util.Util
import dev.castive.jmp.Runner
import dev.castive.jmp.api.App
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Oauth2: EndpointGroup {
	companion object {
		val providers = hashMapOf<String, AbstractOAuth2Provider>()
		init {
			if(EnvUtil.getEnv(EnvUtil.GITHUB_ENABLED) == "true") providers["github"] = GitHubProvider()
			if(EnvUtil.getEnv(EnvUtil.GOOGLE_ENABLED) == "true") providers["google"] = GoogleProvider()
			Log.i(Oauth2::class.java, "Enabled ${providers.size} OAuth2 providers")
			Log.i(Oauth2::class.java, "Active OAuth2 providers: ${Arrays.toString(providers.keys.toTypedArray())}")
		}
	}
	private fun getProvider(ctx: Context): AbstractOAuth2Provider {
		val provider = ctx.queryParam("provider", String::class.java).getOrNull() ?: throw BadRequestResponse("Invalid provider")
		return kotlin.runCatching { providers[provider] }.getOrNull() ?: throw NotFoundResponse("That provider could not be found.")
	}
	override fun addEndpoints() {
		/**
		 * Check whether a provider exists
		 * Used by the front end for showing social login buttons
		 */
		head("${Runner.BASE}/v2/oauth2/authorise", { ctx ->
			val oauth = getProvider(ctx)
			ctx.status(HttpStatus.OK_200).result(oauth.sourceName)
		}, Auth.openAccessRole)
		/**
		 * Redirect the user to an oauth2 provider consent screen
		 * No handling is done here, that is done by the callback
		 * Note: the user will hit this endpoint directly
		 */
		get("${Runner.BASE}/v2/oauth2/authorise", { ctx ->
			val oauth = getProvider(ctx)
			// get the url from the actual provider
			val url = oauth.getAuthoriseUrl()
			ctx.redirect(url, HttpStatus.FOUND_302)
		}, Auth.openAccessRole)
		/**
		 * Refresh the access token using a valid refresh token
		 */
		get("${Runner.BASE}/v2/oauth2/refresh", { ctx ->
			val refresh = ctx.queryParam("refreshToken", String::class.java, null).getOrNull() ?: throw BadRequestResponse("Invalid refresh token")
			val oauth = getProvider(ctx)
			val token = oauth.refreshToken(refresh)
			Log.i(javaClass, "Refreshed access token using refresh token: $refresh")
			// Create a new session
			newSession(token.refreshToken, null, refresh)
			ctx.status(HttpStatus.OK_200).json(Oauth.TokenResponse(token.accessToken, token.refreshToken, oauth.sourceName))
		}, Auth.openAccessRole)
		/**
		 * Use the consent code to get a session from the Oauth provider
		 */
		get("${Runner.BASE}/v2/oauth2/callback", { ctx ->
			// do something with the response
			Log.d(javaClass, "query: ${ctx.queryString()}, path: ${ctx.path()}")
			val code = ctx.queryParam("code", String::class.java).getOrNull()
			if(code == null) {
				// We couldn't get the code from the consent callback
				Log.e(javaClass, "Failed to get code from callback query: ${ctx.queryString()}")
				throw BadRequestResponse("Could not find 'code' query parameter")
			}
			val provider = ctx.header("X-Auth-Source") ?: throw BadRequestResponse("Please set the X-Auth-Source header")
			Log.i(javaClass, "Got provider from header [X-Auth-Source]: $provider")
			val oauth = kotlin.runCatching { providers[provider] }.getOrNull() ?: throw NotFoundResponse("That provider could not be found.")
			val token = oauth.getAccessToken(code)
			Log.d(javaClass, Util.gson.toJson(token))
			// Attempt to create the user
			if(createUser(token, oauth))
				ctx.status(HttpStatus.OK_200).json(Oauth.TokenResponse(token.accessToken, token.refreshToken ?: token.accessToken, oauth.sourceName))
			else {
				Log.e(javaClass, "Failed to create user from token: ${token.accessToken}")
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
			}
		}, Auth.openAccessRole)
		/**
		 * Invalidate a users token
		 */
		post("${Runner.BASE}/v2/oauth2/logout", { ctx ->
			val token = ctx.queryParam("accessToken", String::class.java, null).getOrNull() ?: throw BadRequestResponse("Invalid access token")
			val oauth = getProvider(ctx)
			Log.a(javaClass, "Logging out user with accessToken: $token")
			oauth.revokeTokenAsync(token)
			ctx.status(HttpStatus.OK_200)
		}, Auth.defaultRoleAccess)
	}

	private fun createUser(token: OAuth2AccessToken, provider: AbstractOAuth2Provider): Boolean = createUser(token.accessToken, token.refreshToken ?: token.accessToken, provider)
	/**
	 * Create a user by getting their information from the provider
	 */
	private fun createUser(accessToken: String, refreshToken: String = accessToken, provider: AbstractOAuth2Provider): Boolean {
		val userData = provider.getUserInformation(accessToken)
		if(userData == null) {
			Log.e(javaClass, "Failed to get user information for using with token: $accessToken")
			return false
		}
		// Only create the user if they don't exist
		if(!App.auth.userExists(userData.username)) {
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
		val existingSession = AuthAction.getSession(oldToken)
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