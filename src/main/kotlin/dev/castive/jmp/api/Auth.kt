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
import dev.castive.javalin_auth.auth.Roles.BasicRoles
import dev.castive.jmp.db.dao.Roles
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.existsByUsername
import dev.castive.jmp.util.isEqual
import dev.castive.log2.logi
import dev.castive.log2.logv
import io.javalin.http.ConflictResponse
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import dev.castive.javalin_auth.auth.Roles as AuthRoles
import dev.castive.jmp.db.dao.Role as DaoRole

class Auth {
	companion object {
		val openAccessRole = AuthRoles.openAccessRole
		val defaultRoleAccess = AuthRoles.defaultAccessRole
		val adminRoleAccess = AuthRoles.adminAccessRole
	}

	fun computeHash(password: CharArray): String {
		return Hash.password(password).create()
	}

	internal fun hashMatches(password: CharArray, expectedHash: String): Boolean {
		if(expectedHash.isBlank()) return false
		return Hash.password(password).verify(expectedHash)
	}

	fun createUser(username: String, password: CharArray, admin: Boolean = false, displayName: String = ""): User {
		val hash = computeHash(password)
		if(!Users.existsByUsername(username)) { // Assume the user hasn't been added
			"Attempting to create user: $username, admin=$admin".logi(javaClass)
			return transaction {
				User.new {
					this.username = username
					this.hash = hash
					this.displayName = displayName
					role = if (admin) getDAOAdminRole() else getDAOUserRole()
				}
			}
		}
		else {
			"Failed to create user ($username) as they already exist!".logv(javaClass)
			throw ConflictResponse()
		}
	}

	fun isAdmin(user: User?): Boolean {
		if(user == null) return false
		return transaction { return@transaction user.role.isEqual(BasicRoles.ADMIN) }
	}
	fun getDAOUserRole(): DaoRole {
		return getDAORole(BasicRoles.USER)
	}
	private fun getDAOAdminRole(): DaoRole {
		return getDAORole(BasicRoles.ADMIN)
	}
	private fun getDAORole(role: BasicRoles): DaoRole {
		return transaction {
			return@transaction DaoRole.find {
				Roles.name.lowerCase() eq role.name.toLowerCase()
			}.elementAtOrNull(0)?: // If role is null, create it
			DaoRole.new {
				name = role.name
			}
		}
	}
}
