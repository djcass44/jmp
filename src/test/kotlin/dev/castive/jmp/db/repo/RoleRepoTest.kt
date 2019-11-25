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

import dev.castive.jmp.db.DatabaseTest
import dev.castive.jmp.db.dao.Roles
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class RoleRepoTest: DatabaseTest() {
	@Test
	fun `user role exists`() {
		assertThat(Roles.findAllByName("USER").size, `is`(1))
	}
	@Test
	fun `admin role exists`() {
		assertThat(Roles.findAllByName("ADMIN").size, `is`(1))
	}
	@Test
	fun `unknown role doesn't exist`() {
		assertThat(Roles.findAllByName("FRIENDS").size, `is`(0))
	}
}
