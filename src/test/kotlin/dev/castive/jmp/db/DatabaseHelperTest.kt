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

package dev.castive.jmp.db

import dev.castive.javalin_auth.auth.Roles
import dev.castive.jmp.api.Auth
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.repo.findFirstByUsername
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseHelperTest: DatabaseTest() {
	@Test
	fun `all content tables are empty`() {
		transaction {
			addLogger(StdOutSqlLogger)
			assertThat(Jump.all().empty(), `is`(true))
			assertThat(Group.all().empty(), `is`(true))
			assertThat(Alias.all().empty(), `is`(true))
			assertThat(Session.all().empty(), `is`(true))
			assertThat(Meta.all().empty(), `is`(true))
		}
	}

	@Test
	fun `correct number of roles exist`() {
		transaction {
			addLogger(StdOutSqlLogger)
			assertThat(Role.all().count(), `is`(Roles.BasicRoles.values().size))
		}
	}

	@Test
	fun `default users are created`() {
		transaction {
			addLogger(StdOutSqlLogger)
			assertThat(User.all().count(), `is`(2))
			assertThat(Users.findFirstByUsername("admin"), notNullValue())
			assertThat(Users.findFirstByUsername("system"), notNullValue())
		}
	}

	@Test
	fun `admin user is an admin`() {
		val admin = Users.findFirstByUsername("admin")
		assertNotNull(admin)
		assertTrue(Auth().isAdmin(admin))
	}

	@Test
	fun `system user is not an admin`() {
		val system = Users.findFirstByUsername("system")
		assertNotNull(system)
		assertFalse(Auth().isAdmin(system))
	}
}
