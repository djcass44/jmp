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

package dev.castive.jmp.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.castive.jmp.util.UUIDConverterCompat
import dev.castive.log2.logv
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "Users")
data class User(
	@Id
	@Convert(converter = UUIDConverterCompat::class)
	val id: UUID = UUID.randomUUID(),
	@Column(unique = true, nullable = false)
	val username: String,
	var displayName: String,
	var avatarUrl: String? = null,
	@JsonIgnore
	val hash: String? = null,
	@ElementCollection(fetch = FetchType.EAGER)
	val roles: MutableList<Role> = mutableListOf(Role.ROLE_USER),
	@OneToOne
	val meta: Meta,
	val source: String
) {
	fun isAdmin(): Boolean = roles.contains(Role.ROLE_ADMIN)

	fun addRole(role: Role) {
		// only add the role if it isn't already there
		if(!roles.contains(role)) {
			"Adding role ${role.name} to user $username".logv(javaClass)
			roles.add(role)
		}
	}
}
