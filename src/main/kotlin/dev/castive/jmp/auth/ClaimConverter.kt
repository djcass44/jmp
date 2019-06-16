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

import dev.castive.javalin_auth.actions.UserAction
import dev.castive.javalin_auth.auth.external.ValidUserClaim
import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.api.App
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.log2.Log
import io.javalin.Context
import io.javalin.UnauthorizedResponse
import org.jetbrains.exposed.sql.transactions.transaction

object ClaimConverter {
	fun getUser(ctx: Context): User? {
		return getUser(UserAction.getOrNull(ctx), ctx)
	}

	/**
	 * Try to determine who the user is based on whatever we can dig up
	 * 1. Try to see if the claim can determine the user
	 * 2. Check to see if the user is in the cache layer
	 * 3. If not, check again?
	 * 4. Check to see if the external provider knows them
	 * 5. Additional Crowd checks
	 */
	fun getUser(claim: ValidUserClaim?, ctx: Context): User? {
		val ssoToken = kotlin.runCatching {
			return@runCatching ctx.cookie(App.crowdCookieConfig!!.name)
		}.getOrNull()
		val user: User? = transaction {
			return@transaction if (claim == null) null
			else {
				val u = User.find { Users.username eq claim.username }.elementAtOrNull(0)
				val token = ssoToken ?: claim.token
				val hasToken = AuthAction.userHadToken(u?.username, token)
				if(hasToken == null && u != null && token != u.id.value.toString()) {
					return@transaction u
				}
				else return@transaction if(hasToken != null) u else null
			}
		}
		Log.v(javaClass, "User is provided: ${user != null && user.from != InternalProvider.SOURCE_NAME}")
		if(user != null && AuthAction.cacheLayer.connected()) {
			val cached = AuthAction.cacheLayer.getUser(user.id.value, claim?.token ?: user.id.value.toString())
			Log.d(javaClass, "Got user cache: $cached")
			// How do we deal with stale results?
			if(cached != null) {
				Log.w(javaClass, "We are using a cached user claim for ${user.id.value}")
				return user
			}
		}
		else if(user == null && ssoToken != null) {
			// Check the cache first
			val uid = AuthAction.cacheLayer.getUser(ssoToken)
			if(uid != null) {
				// We've found a possible user, lets make sure they exist first
				val u = transaction { User.findById(uid) }
				if(u != null) {
					Log.i(javaClass, "We found a cached user, let's try again")
					return getUser(ValidUserClaim(u.username, ssoToken), ctx)
				}
			}
			// Check to see if the external provider knows who the user is by the given token
			val externalUser = AuthAction.getTokenInfo(ssoToken, ctx)
			if(externalUser != null) {
				// We know who the user is now! Let's start over
				Log.i(javaClass, "External provider knows the user, let's try again")
				return getUser(ValidUserClaim(externalUser.user.name, externalUser.token), ctx)
			}
		}
		return if(App.crowdCookieConfig != null && user != null && user.from != InternalProvider.SOURCE_NAME) {
			// Check for Crowd SSO cookie
			if(ssoToken == null || ssoToken.isBlank()) {
				Log.v(javaClass, "Failed to get CrowdCookie: $ssoToken")
				AuthAction.onUserValid(user, null)
				user
			}
			else {
				// Check that crowd is aware of our token
				val token = AuthAction.getTokenInfo(ssoToken, ctx) ?: return null
				// Get the user from Crowds response
				// We know that auth MUST be valid otherwise Crowd would not return 200 OK
				if(ssoToken != token.token) Log.a(javaClass, "Current token and new token don't match, Crowd must have issued a refresh!")
				val foundUser = App.auth.getUser(token.user.name)
				transaction {
					// Update the users token to match Crowd
					Log.d(javaClass, "Updating user with discovered SSO token: ${claim?.username}, ${token.user.name}")
					val s = AuthAction.userHadToken(foundUser?.username, ssoToken)
					s?.ssoToken = token.token
				}
				Log.d(javaClass, "Searching for active user with token: [cur: $ssoToken, new: ${token.token}]: ${foundUser?.username}")
				if(foundUser != null) AuthAction.onUserValid(foundUser, token.token)
				foundUser
			}
		}
		else {
			Log.v(javaClass, "Returning user we found locally: ${user?.username}")
			if(user != null) AuthAction.onUserValid(user, null)
			user
		}
	}

	/**
	 * Get a users SSO cookie, if it exists
	 */
	fun getToken(ctx: Context): String? = kotlin.runCatching {
		ctx.cookie(App.crowdCookieConfig!!.name)
	}.getOrNull()
	fun get(ctx: Context): User {
		return get(UserAction.getOrNull(ctx), ctx)
	}
	fun get(claim: ValidUserClaim?, ctx: Context): User {
		return getUser(claim, ctx) ?: throw UnauthorizedResponse("Invalid token")
	}
}