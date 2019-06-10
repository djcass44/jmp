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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

object ClaimConverter {
	fun getUser(ctx: Context): User? {
		return getUser(UserAction.getOrNull(ctx), ctx)
	}
	fun getUser(claim: ValidUserClaim?, ctx: Context): User? {
		val user = transaction {
			return@transaction if (claim == null) null
			else User.find {
				Users.username eq claim.username and(Users.requestToken.eq(claim.token))
			}.elementAtOrNull(0)
		}
		Log.v(javaClass, "User is provided: ${user?.from != InternalProvider.SOURCE_NAME}")
		return if(App.crowdCookieConfig != null && user?.from != InternalProvider.SOURCE_NAME) {
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
				// This should always be true
				if(ssoToken == token.token) {
					// Get the user from Crowds response
					val foundUser = App.auth.getUser(token.user.name)
					transaction {
						// Update the users token to match Crowd
						Log.d(javaClass, "Updating user with discovered SSO token: ${claim?.username}, ${token.user.name}")
						foundUser?.requestToken = token.token
					}
				}
				Log.d(javaClass, "Searching for active user with token: $ssoToken")
				App.auth.getUserWithSSOToken(ssoToken)
			}
		}
		else {
			Log.v(javaClass, "Returning user we found locally")
			user
		}
	}
	fun get(ctx: Context): User {
		return get(UserAction.getOrNull(ctx), ctx)
	}
	fun get(claim: ValidUserClaim?, ctx: Context): User {
		return getUser(claim, ctx) ?: throw UnauthorizedResponse("Invalid token")
	}
}