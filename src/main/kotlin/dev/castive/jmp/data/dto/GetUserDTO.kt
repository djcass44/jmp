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

package dev.castive.jmp.data.dto

import dev.castive.jmp.entity.Role
import dev.dcas.jmp.security.shim.entity.Meta
import dev.dcas.jmp.security.shim.entity.User
import org.springframework.security.core.GrantedAuthority
import java.util.UUID

data class GetUserDTO(
	val id: UUID,
	val username: String,
	val displayName: String,
	val avatarUrl: String? = null,
	val roles: List<GrantedAuthority>,
	val meta: Meta
) {
	fun isAdmin(): Boolean = roles.contains(Role.ROLE_ADMIN)

	constructor(user: User, requester: User): this(
		user.id,
		user.username,
		if(requester.source == user.source) user.displayName else user.username,
		if(requester.source == user.source) user.avatarUrl else null,
		user.roles,
		user.meta
	)
}
