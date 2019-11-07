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

package dev.castive.jmp.auth

import dev.castive.javalin_auth.api.OAuth2
import dev.castive.javalin_auth.auth.Providers
import dev.castive.jmp.api.App
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.findFirstByRefreshTokenAndActive
import dev.castive.jmp.db.repo.findFirstByUsername
import dev.castive.log2.Log
import dev.castive.log2.logd
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import dev.castive.javalin_auth.auth.data.User as AuthUser

object ClaimConverter {

	fun getUser(ctx: Context, userUtils: UserUtils): User? {
		var user: AuthUser?
		var user2: User? = null
		// See if the primary provider can find the user
		user = getPrimary(ctx)
		// Fall-back to the internal provider
		if(user == null) {
			user = getSecondary(ctx)
		}
		// try to find an OAuth user
		if(user == null) {
			user2 = getOAuth2(ctx, userUtils)
		}
		if(user == null && user2 == null) {
			Log.i(javaClass, "[${ctx.path()}] Failed to locate user with any provider")
			return null
		}
		return user2 ?: Users.findFirstByUsername(user!!.username)
	}

	/**
	 * Try to find a user in the primary provider
	 */
	private fun getPrimary(ctx: Context): AuthUser? {
		if(Providers.primaryProvider != null) {
			Log.v(javaClass, "Searching context for user with provider: ${Providers.primaryProvider!!::class.java.name}")
			val res = Providers.primaryProvider!!.hasUser(ctx)
			res.first?.let {
				Log.d(javaClass, "Discovered external user: ${it.username}")
			}
			return res.first
		}
		return null
	}

	/**
	 * Try to find a user in the internal provider
	 */
	private fun getSecondary(ctx: Context): AuthUser? {
		Log.v(javaClass, "Searching context for user with fallback provider: ${Providers.internalProvider::class.java.name}")
		val res = Providers.internalProvider.hasUser(ctx)
		res.first?.let {
			Log.d(javaClass, "Discovered internal user: ${it.username}")
		}
		return res.first
	}

	/**
	 * Try to find a user from an OAuth2 provider
	 */
	private fun getOAuth2(ctx: Context, userUtils: UserUtils): User? {
		Log.v(javaClass, "Searching context for user with Oauth provider")
		// TRY to get the accessToken from the Authorization header
		val header = getAuthHeader(ctx)
		if(header != null) {
			Log.v(javaClass, "Got accessToken: $header")
			// Check that the accessToken is still valid
			val maybeUsername = userUtils.cache.getUser(header)
			val maybeUser = if(maybeUsername != null) Users.findFirstByUsername(maybeUsername.username) else null
			if(maybeUser != null) {
				Log.i(javaClass, "Found user in cache: ${maybeUsername?.username}")
				return maybeUser
			}
			else {
				val source = OAuth2.providers[ctx.header("X-Auth-Source")]
				"Unable to find user in cache, searching manually via provider: ${source?.sourceName}".logd(javaClass)
				if(source != null) {
					if (source.isTokenValid(header)) {
						Log.v(javaClass, "AccessToken is valid, lets get its session")
						// Get the session the accessToken belongs to
						val session = Sessions.findFirstByRefreshTokenAndActive(header)
						// Get the user from the session
						if (session != null) {
							val user = transaction {
								session.user
							}
							userUtils.onUserValid(user, header)
							Log.i(javaClass, "Found session for ${user.username}")
							return user
						}
					}
					else {
						"Got invalid token: $header".logd(javaClass)
						userUtils.onUserInvalid(header)
					}
				}
			}
		}
		return null
	}

	/**
	 * Get the 'Authorization' header from the network request
	 */
	private fun getAuthHeader(ctx: Context): String? = kotlin.runCatching {
		ctx.header("Authorization")!!.split(" ")[1]
	}.getOrNull()

	/**
	 * Get a users SSO cookie, if it exists
	 */
	fun getToken(ctx: Context): String? = kotlin.runCatching {
		ctx.cookie(App.crowdCookieConfig!!.name)
	}.getOrNull()
}
