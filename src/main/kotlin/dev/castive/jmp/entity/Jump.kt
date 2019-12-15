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

import javax.persistence.*

@Entity
@Table(name = "Jumps")
data class Jump(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Int,
	var name: String,
	var location: String,
	var title: String? = null,
	@OneToMany
	val alias: MutableSet<Alias> = mutableSetOf(),
	@ManyToOne
	val owner: User? = null,
	@ManyToOne
	val ownerGroup: Group? = null,
	var image: String? = null,
	@OneToOne
	val meta: Meta,
	var usage: Int = 0
) {
	companion object {
		const val TYPE_GLOBAL = 0
		const val TYPE_PERSONAL = 1
		const val TYPE_GROUP = 2

		fun getType(jump: Jump): Int = when {
			jump.isPublic() -> TYPE_GLOBAL
			jump.owner != null -> TYPE_PERSONAL
			jump.ownerGroup != null -> TYPE_GROUP
			else -> TYPE_GLOBAL
		}
	}
	fun isPublic(): Boolean = owner == null && ownerGroup == null
}
