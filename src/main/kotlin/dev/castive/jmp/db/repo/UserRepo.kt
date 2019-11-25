/*
 *    Copyright [2019 Django Cass
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

package dev.castive.jmp.db.repo

import dev.castive.javalin_auth.auth.data.User2
import dev.castive.javalin_auth.auth.data.UserEntity
import dev.castive.jmp.api.App
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * Get the number of stored users
 */
fun Users.count(): Int = transaction {
	User.all().count()
}

fun Users.findFirstByEntity(entity: UserEntity<UUID>): User? = transaction {
	User.findById(entity.id)
}

/**
 * Create a new user from an Authentication claim
 * This must only be used for federated identities
 */
fun Users.new(user: User2): User = transaction {
	return@transaction User.new {
		username = user.username
		displayName = user.displayName
		avatarUrl = user.avatarUrl
		hash = ""
		role = App.auth.getDAOUserRole() // assume user for now
		from = user.source
	}
}

/**
 * Check whether a user exists or not
 * @param username: the username to check, not case sensitive
 */
fun Users.existsByUsername(username: String): Boolean = transaction {
	!User.find {
		Users.username.lowerCase() eq username.toLowerCase()
	}.empty()
}

/**
 * Find all users NOT from a given provider
 */
fun Users.findAllNotFrom(from: String): List<User> = transaction {
	User.find {
		Users.from neq from
	}.toList()
}

fun Users.findAllByUsername(username: String): List<User> = transaction {
	User.find {
		Users.username eq username
	}.toList()
}
/**
 * Get a user by their username
 */
fun Users.findFirstByUsername(username: String): User? = transaction {
	User.find {
		Users.username eq username
	}.elementAtOrNull(0)
}

fun Users.findFirstByUsernameAndSource(username: String, source: String): User? = transaction {
	User.find {
		Users.username eq username and from.eq(source)
	}.elementAtOrNull(0)
}
