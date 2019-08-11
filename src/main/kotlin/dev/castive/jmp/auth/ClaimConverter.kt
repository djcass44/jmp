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
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.log2.Log
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import dev.castive.javalin_auth.auth.data.User as AuthUser

object ClaimConverter {
	fun getUser(ctx: Context, userUtils: UserUtils): User? {
		var user: AuthUser? = null
		var user2: User? = null
		// See if the primary provider can find the user
		if(Providers.primaryProvider != null) {
			Log.v(javaClass, "Searching context for user with provider: ${Providers.primaryProvider!!::class.java.name}")
			val res = Providers.primaryProvider!!.hasUser(ctx)
			user = res.first
			Log.d(javaClass, "Discovered external user: ${user?.username}")
		}
		// Fall-back to the internal provider
		if(user == null) {
			Log.v(javaClass, "Searching context for user with fallback provider: ${Providers.internalProvider::class.java.name}")
			val res = Providers.internalProvider.hasUser(ctx)
			user = res.first
			Log.d(javaClass, "Discovered internal user: ${user?.username}")
		}
		if(user == null) {
			Log.v(javaClass, "Searching context for user with Oauth provider")
			// TRY to get the accessToken from the Authorization header
			val header = kotlin.runCatching { ctx.header("Authorization")!!.split(" ")[1] }.getOrNull()
			if(header != null) {
				Log.v(javaClass, "Got accessToken: $header")
				// Check that the accessToken is still valid
				val maybeUsername = userUtils.cache.getUser(header)
				val maybeUser = if(maybeUsername != null) userUtils.getUser(maybeUsername.username) else null
				if(maybeUser != null) {
					Log.i(javaClass, "Found user in cache: ${maybeUsername?.username}")
					user2 = maybeUser
				}
				else {
					val source = OAuth2.providers[ctx.header("X-Auth-Source")]
					if(source != null) {
						val res = source.isTokenValid(header)
						if (res) {
							Log.v(javaClass, "AccessToken is valid, lets get its session")
							// Get the session the accessToken belongs to
							val session = userUtils.getSession(header)
							// Get the user from the session
							if (session != null) {
								user2 = transaction { session.user }
								userUtils.onUserValid(user2, header)
								Log.i(javaClass, "Found session for ${user2.username}")
							}
						} else
							userUtils.onUserInvalid(header)
					}
				}
			}
		}
		if(user == null && user2 == null) {
			Log.i(javaClass, "Failed to locate user with any provider")
			return null
		}
		return user2 ?: transaction {
			User.find {
				// user2 is null so user cannot be null
				Users.username eq user!!.username
			}.elementAtOrNull(0)
		}
	}

	/**
	 * Get a users SSO cookie, if it exists
	 */
	fun getToken(ctx: Context): String? = kotlin.runCatching {
		ctx.cookie(App.crowdCookieConfig!!.name)
	}.getOrNull()
}