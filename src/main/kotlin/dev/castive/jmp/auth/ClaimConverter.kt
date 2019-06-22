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

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.provider.BaseProvider
import dev.castive.jmp.api.App
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.log2.Log
import io.javalin.Context
import org.jetbrains.exposed.sql.transactions.transaction
import dev.castive.javalin_auth.auth.data.User as AuthUser

object ClaimConverter {
	fun getUser(ctx: Context): User? {
		var user: AuthUser? = null
		var token: BaseProvider.TokenContext? = null
		// See if the primary provider can find the user
		if(Providers.primaryProvider != null) {
			Log.v(javaClass, "Searching context for user with provider: ${Providers.primaryProvider!!::class.java.name}")
			val res = Providers.primaryProvider!!.hasUser(ctx)
			user = res.first
			token = res.second
		}
		// Fall-back to the internal provider
		if(user == null) {
			Log.v(javaClass, "Searching context for user with fallback provider: ${Providers.internalProvider::class.java.name}")
			val res = Providers.internalProvider.hasUser(ctx)
			user = res.first
			token = res.second
		}
		if(user == null) {
			Log.i(javaClass, "Failed to locate user with any provider")
			return null
		}
		val actualUser = transaction {
			User.find {
				Users.username eq user.username
			}.elementAtOrNull(0)
		}
		if(actualUser != null && token != null && token.current.isNotBlank()) {
			transaction {
				// Update the users token to match Crowd
				Log.d(javaClass, "Updating user with discovered SSO token: ${actualUser.username}, ${token.last}")
				val s = AuthAction.userHadToken(actualUser.username, token.last)
				s?.ssoToken = token.current
			}
			// Update the cache layer
			AuthAction.onUserValid(actualUser, token.current)
		}
		else if(actualUser != null) AuthAction.onUserValid(actualUser, null)
		return actualUser
	}

	/**
	 * Get a users SSO cookie, if it exists
	 */
	fun getToken(ctx: Context): String? = kotlin.runCatching {
		ctx.cookie(App.crowdCookieConfig!!.name)
	}.getOrNull()
}