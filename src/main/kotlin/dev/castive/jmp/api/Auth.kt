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

package dev.castive.jmp.api

import com.amdelamar.jhash.Hash
import dev.castive.javalin_auth.auth.Providers
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.db.dao.Roles
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.log2.Log
import io.javalin.ConflictResponse
import io.javalin.Context
import io.javalin.security.Role
import io.javalin.security.SecurityUtil
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import dev.castive.jmp.db.dao.Role as DaoRole

class Auth {
	enum class BasicRoles: Role {
		USER, ADMIN, ANYONE
	}
	companion object {

		val openAccessRole = SecurityUtil.roles(
			BasicRoles.ANYONE,
			BasicRoles.USER,
			BasicRoles.ADMIN
		)
		val defaultRoleAccess = SecurityUtil.roles(
			BasicRoles.USER,
			BasicRoles.ADMIN
		)
		val adminRoleAccess = SecurityUtil.roles(BasicRoles.ADMIN)
		val roleMapping = hashMapOf<String, Role>(
			Pair(BasicRoles.ANYONE.name, BasicRoles.ANYONE),
			Pair(BasicRoles.USER.name, BasicRoles.USER),
			Pair(BasicRoles.ADMIN.name, BasicRoles.ADMIN)
		)
	}

	@Deprecated(message = "Do not use strings when dealing with passwords.")
	fun computeHash(password: String): String {
		return computeHash(password.toCharArray())
	}
	fun computeHash(password: CharArray): String {
		return Hash.password(password).create()
	}
	@Deprecated(message = "Do not use strings when dealing with passwords.")
	fun hashMatches(password: String, expectedHash: String): Boolean {
		if(expectedHash.isBlank()) return false
		return hashMatches(password.toCharArray(), expectedHash)
	}
	private fun hashMatches(password: CharArray, expectedHash: String): Boolean {
		if(expectedHash.isBlank()) return false
		return Hash.password(password).verify(expectedHash)
	}

	fun createUser(username: String, password: CharArray, admin: Boolean = false) {
		val hash = computeHash(password)
		if(!userExists(username)) { // Assume the user hasn't been added
			transaction {
				User.new {
					this.username = username
					this.hash = hash
					role = if(admin) getDAOAdminRole() else getDAOUserRole()
					metaCreation = System.currentTimeMillis()
					metaUpdate = System.currentTimeMillis()
				}
			}
		}
		else
			throw ConflictResponse()
	}
	fun loginUser(token: String, ctx: Context): String? {
		Log.v(javaClass, "Attempting to login user via SSO token")
		if(Providers.primaryProvider != null) {
			val primaryAttempt = AuthAction.isValidToken(token, ctx)
			if(primaryAttempt.isNotEmpty()) {
				Log.v(javaClass, "Found user using token: $primaryAttempt")
				return primaryAttempt
			}
		}
		return null
	}
	fun loginUser(username: String, password: String): String? {
		Log.v(javaClass, "Attempting to login user via basic authentication")
		var result: String?
		if(Providers.primaryProvider != null) { // Try to use primary provider if it exists
			val primaryAttempt = runCatching { Providers.primaryProvider?.getLogin(username, password) }
			// This is for logging
//			App.exceptionTracker.onExceptionTriggered(primaryAttempt.exceptionOrNull() ?: Exception("Failed to load actual exception class"))
			result = primaryAttempt.getOrNull()
			if(result != null) {
				Log.v(javaClass, "Found user in primary provider: $result")
				return result
			}
		}
		Log.v(javaClass, "Failed to locate user in primary provider, checking local provider")
		// Fallback to internal database checks
		result = Providers.internalProvider.getLogin(username, password)
		if(result != null && result.isBlank()) result = null
		Log.d(javaClass, "Found local user: $result")
		return result
	}

	fun validateUserToken(token: UUID): Boolean {
		return transaction {
			val existing = User.findById(token)
			return@transaction existing != null
		}
	}

	fun getUserToken(username: String, password: CharArray): String? {
		assert(userExists(username))
		return transaction {
			val existing = User.find {
				Users.username eq username
			}.limit(1)
			val user = existing.elementAtOrNull(0)
			// Only return if hashes match (and user was found)
			return@transaction if(user != null && hashMatches(password, user.hash))
				user.id.value.toString()
			else
				null
		}
	}

	/**
	 * Get a users token without checking password
	 * ONLY USE THIS ONCE EXTERNAL AUTHENTICATION HAS SUCCEEDED
	 */
	fun getUserTokenWithPrivilege(username: String): String {
		assert(userExists(username)) // Requires users to be synced already
		return transaction {
			val existing = User.find {
				Users.username eq username
			}.limit(1)
			val user = existing.elementAtOrNull(0)
			return@transaction user?.id?.value.toString()
		}
	}
	fun userExists(username: String): Boolean {
		return transaction {
			val existing = User.find {
				Users.username.lowerCase() eq username.toLowerCase()
			}
			return@transaction !existing.empty()
		}
	}

	fun getUser(username: String): User? {
		return transaction {
			return@transaction User.find {
				Users.username eq username
			}.elementAtOrNull(0)
		}
	}
	fun getUserWithSSOToken(token: String): User? {
		return transaction {
			return@transaction User.find {
				Users.requestToken eq token
			}.elementAtOrNull(0)
		}
	}
	fun getUser(username: String, token: String): User? {
		if(username.isBlank() || token.isBlank()) return null
		return transaction {
			return@transaction User.find {
				Users.username eq username and(Users.requestToken.eq(token))
			}.elementAtOrNull(0)
		}
	}
	fun getUser(username: String?, token: UUID?): User? {
		if(username == null || token == null)
			return null
		return transaction {
			return@transaction User.findById(token)
		}
	}

	// Determine role based on request
	fun getUserRole(username: String? = null): Role {
		if(username == null)
			return BasicRoles.USER
		val user = getUser(username) ?: return BasicRoles.USER
		return transaction {
			when (user.role.name) {
				BasicRoles.ADMIN.name -> BasicRoles.ADMIN
				BasicRoles.USER.name -> BasicRoles.USER
				else -> BasicRoles.USER
			}
		}
	}
	// Determine role based on request
	fun getUserRole(username: String, token: UUID): Role {
		val user = getUser(username) ?: return BasicRoles.USER
		if(user.id.value != token) {
			Log.w(javaClass, "getUserRole -> $user provided invalid token")
			return BasicRoles.USER
		}
		return transaction {
			when (user.role.name) {
				BasicRoles.ADMIN.name -> BasicRoles.ADMIN
				BasicRoles.USER.name -> BasicRoles.USER
				else -> BasicRoles.USER
			}
		}
	}
	fun isAdmin(user: User): Boolean = transaction {
		return@transaction user.role.name == getDAOAdminRole().name
	}
	fun getDAOUserRole(): DaoRole {
		return getDAORole(BasicRoles.USER)
	}
	fun getDAOAdminRole(): DaoRole {
		return getDAORole(BasicRoles.ADMIN)
	}
	private fun getDAORole(role: BasicRoles): DaoRole {
		return transaction {
			return@transaction DaoRole.find {
				Roles.name eq role.name
			}.elementAtOrNull(0)?: // If role is null, create it
			DaoRole.new {
				name = role.name
			}
		}
	}
}