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
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.AuthenticateResponse
import dev.castive.javalin_auth.auth.external.ValidUserClaim
import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.api.App
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.util.SystemUtil
import dev.castive.log2.Log
import io.javalin.Context
import io.javalin.UnauthorizedResponse
import org.jetbrains.exposed.sql.transactions.transaction

object ClaimConverter {
	fun getUser(ctx: Context): User? {
		return getUser(UserAction.getOrNull(ctx), ctx)
	}
	fun getUser(claim: ValidUserClaim?, ctx: Context): User? {
		val user: User? = transaction {
			return@transaction if (claim == null) null
			else {
				val u = User.find {
					// TODO this fails if Crowd issues a refreshed token
					Users.username eq claim.username
				}.elementAtOrNull(0)
				return@transaction if(AuthAction.userHadToken(u?.username, claim.token) != null) u else null
			}
		}
		Log.v(javaClass, "User is provided: ${user != null && user.from != InternalProvider.SOURCE_NAME}")
		return if(App.crowdCookieConfig != null && user != null && user.from != InternalProvider.SOURCE_NAME) {
			// Check for Crowd SSO cookie
			val ssoToken = kotlin.runCatching {
				return@runCatching ctx.cookie(App.crowdCookieConfig!!.name)
			}.getOrNull()
			if(ssoToken == null || ssoToken.isBlank()) {
				Log.v(javaClass, "Failed to get CrowdCookie: $ssoToken")
				user
			}
			else {
				// Check that crowd is aware of our token
				val token = kotlin.runCatching {
					SystemUtil.gson.fromJson(AuthAction.isValidToken(ssoToken, ctx), AuthenticateResponse::class.java)
				}.getOrNull() ?: return null
				// Get the user from Crowds response
				// We know that auth MUST be valid otherwise Crowd would not return 200 OK
				if(ssoToken != token.token) Log.a(javaClass, "Current token and new token don't match, Crowd must have issued a refresh!")
				val foundUser = App.auth.getUser(token.user.name)
				transaction {
					// Update the users token to match Crowd
					Log.d(javaClass, "Updating user with discovered SSO token: ${claim?.username}, ${token.user.name}")
					foundUser?.requestToken = token.token
				}
				Log.d(javaClass, "Searching for active user with token: [cur: $ssoToken, new: ${token.token}]")
				App.auth.getUserWithSSOToken(token.token)
			}
		}
		else {
			Log.v(javaClass, "Returning user we found locally")
			user
		}
	}
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