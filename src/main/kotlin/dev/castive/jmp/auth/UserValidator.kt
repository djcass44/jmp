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

import dev.castive.javalin_auth.auth.connect.LDAPConfig
import dev.castive.javalin_auth.auth.data.Group
import dev.castive.javalin_auth.auth.data.User
import dev.castive.javalin_auth.auth.external.UserIngress
import dev.castive.javalin_auth.auth.provider.LDAPProvider
import dev.castive.jmp.api.Auth
import dev.castive.jmp.db.dao.Groups
import dev.castive.jmp.db.dao.Users
import dev.castive.log2.Log
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import dev.castive.jmp.db.dao.Group as DaoGroup
import dev.castive.jmp.db.dao.User as DaoUser

class UserValidator(private val auth: Auth, private val ldapConfigExtras: LDAPConfig.Extras): UserIngress {
	override fun ingestGroups(groups: ArrayList<Group>) {
		val names = arrayListOf<String>()
		transaction {
			groups.forEach { g ->
				names.add(g.name)
				val match = DaoGroup.find { Groups.name eq g.name and Groups.from.eq(g.source) }
				// Get the users in the group
				val users = arrayListOf<DaoUser>()
				g.members.forEach { users.add(getOrCreateUser(it)) }
				if(match.empty()) {
					// Group doesn't exist yet
					DaoGroup.new {
						name = g.name
						from = g.source
						this.users = SizedCollection(users)
					}
				}
				else {
					// Update the existing group
					match.elementAt(0).apply {
						this.users = SizedCollection(users)
					}
				}
			}
			val externalGroups = DaoGroup.find { Groups.from eq LDAPProvider.SOURCE_NAME }
			val invalid = arrayListOf<DaoGroup>()
			externalGroups.forEach { if(!names.contains(it.name)) invalid.add(it) }
			Log.i(javaClass, "Found ${invalid.size} stale groups")
			if(ldapConfigExtras.removeStale) {
				invalid.forEach { it.delete() }
				if(invalid.size > 0) Log.w(javaClass, "Removed ${invalid.size} stale groups")
			}
			else Log.i(javaClass, "Stale group removal blocked by application policy")
		}
	}

	private fun getOrCreateUser(user: User): DaoUser = transaction {
		val match = DaoUser.find { Users.username eq user.username and Users.from.eq(user.source) }
		if(match.empty()) {
			return@transaction DaoUser.new {
				username = user.username
				hash = ""
				role = auth.getDAOUserRole()
				from = user.source
			}
		}
		else return@transaction match.elementAt(0)
	}

	override fun ingestUsers(users: ArrayList<User>) {
		val names = arrayListOf<String>()
		transaction {
			users.forEach { u ->
				names.add(u.username)
				getOrCreateUser(u)
			}
			// Get LDAP users which weren't in the most recent search and delete them
			val externalUsers = DaoUser.find { Users.from eq LDAPProvider.SOURCE_NAME }
			val invalid = arrayListOf<DaoUser>()
			externalUsers.forEach { if(!names.contains(it.username)) invalid.add(it) }
			Log.i(javaClass, "Found ${invalid.size} stale users")
			if(ldapConfigExtras.removeStale) {
				invalid.forEach { it.delete() }
				if(invalid.size > 0) Log.w(javaClass, "Removed ${invalid.size} stale users")
			}
			else Log.i(javaClass, "Stale user removal blocked by application policy")
		}
	}
}