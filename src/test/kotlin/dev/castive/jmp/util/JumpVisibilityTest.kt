/*
 *    Copyright 2020 Django Cass
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

package dev.castive.jmp.util

import dev.castive.jmp.entity.Jump
import dev.castive.jmp.entity.Role
import dev.dcas.jmp.security.shim.entity.Group
import dev.dcas.jmp.security.shim.entity.Meta
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.jmp.spring.security.SecurityConstants
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JumpVisibilityTest {

	@Test
	fun `public jump can be seen by anonymous user`() {
		val jump = Jump(
			0,
			"test",
			"https://test.com",
			null,
			mutableSetOf(),
			null,
			null,
			null,
			Meta(
				0,
				createdBy = UUID.randomUUID(),
				editedBy = UUID.randomUUID()
			)
		)
		assertTrue(jump.isVisibleTo(null))
	}

	@Test
	fun `public-group jump can be seen by anonymous user`() {
		val group = Group(
			UUID.randomUUID(),
			"test",
			SecurityConstants.sourceLocal,
			true,
			null,
			mutableSetOf()
		)
		val jump = Jump(
			0,
			"test",
			"https://test.com",
			null,
			mutableSetOf(),
			null,
			group,
			null,
			Meta(
				0,
				createdBy = UUID.randomUUID(),
				editedBy = UUID.randomUUID()
			)
		)
		assertTrue(jump.isVisibleTo(null))
	}

	@Test
	fun `group jump cannot be seen by anonymous user`() {
		val group = Group(
			UUID.randomUUID(),
			"test",
			SecurityConstants.sourceLocal,
			false,
			null,
			mutableSetOf()
		)
		val jump = Jump(
			0,
			"test",
			"https://test.com",
			null,
			mutableSetOf(),
			null,
			group,
			null,
			Meta(
				0,
				createdBy = UUID.randomUUID(),
				editedBy = UUID.randomUUID()
			)
		)
		assertFalse(jump.isVisibleTo(null))
	}

	@Test
	fun `user jump cannot be seen by anonymous user`() {
		val user = User(
			UUID.randomUUID(),
			"test",
			"Test User",
			null,
			null,
			mutableListOf(Role.ROLE_USER),
			Meta(
				0,
				createdBy = UUID.randomUUID(),
				editedBy = UUID.randomUUID()
			),
			SecurityConstants.sourceLocal
		)
		val jump = Jump(
			0,
			"test",
			"https://test.com",
			null,
			mutableSetOf(),
			user,
			null,
			null,
			Meta(
				0,
				createdBy = UUID.randomUUID(),
				editedBy = UUID.randomUUID()
			)
		)
		assertFalse(jump.isVisibleTo(null))
		assertTrue(jump.isVisibleTo(user))
	}
}
