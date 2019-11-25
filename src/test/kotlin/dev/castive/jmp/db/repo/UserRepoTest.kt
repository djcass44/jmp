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
import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.api.Auth
import dev.castive.jmp.db.DatabaseTest
import dev.castive.jmp.db.dao.Users
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class UserRepoTest: DatabaseTest() {
	private val auth = Auth()

	@Test
	fun `find user by username`() {
		val username = "user1"
		val name = "John Smith"
		val password = "hunter2"
		val user = auth.createUser(username, password.toCharArray(), false, name)

		// user exists
		assertTrue(Users.existsByUsername(username))
		assertThat(Users.findFirstByUsername(username)?.id?.value, `is`(user.id.value))
		assertThat(Users.findAllByUsername(username).size, `is`(1))
	}

	@Test
	fun `find non-local users`() {
		val username = "user1"
		val name = "John Smith"
		val password = "hunter2"
		val user = auth.createUser(username, password.toCharArray(), false, name)
		val user2 = auth.createUser("user2", password.toCharArray(), false, name)
		transaction {
			user2.from = "ldap"
		}

		assertThat(Users.findAllNotFrom(InternalProvider.SOURCE_NAME).size, `is`(1))
	}

	@Test
	fun `find by entity`() {
		val username = "user1"
		val name = "John Smith"
		val password = "hunter2"
		val user = auth.createUser(username, password.toCharArray(), false, name)
		val entity = UserEntity(user.id.value, username, InternalProvider.SOURCE_NAME)

		assertThat(Users.findFirstByEntity(entity)?.id?.value, `is`(user.id.value))
	}


	@Test
	fun `create user throws nothing`() {
		val username = "user1"
		val name = "John Smith"
		val user = Users.new(User2(username, name, "", "github", "user"))
		assertThat(user.username, `is`(username))
	}
}
