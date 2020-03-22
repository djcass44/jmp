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

import dev.dcas.jmp.security.shim.entity.Group
import dev.dcas.jmp.security.shim.entity.Meta
import dev.dcas.jmp.security.shim.entity.User
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import javax.persistence.*

@Entity
@Indexed
@Table(name = "Jumps")
data class Jump(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Int,
	@Field
	var name: String,
	@Field
	var location: String,
	@Field
	var title: String? = null,
	@OneToMany
	val alias: MutableSet<Alias> = mutableSetOf(),
	@ManyToOne
	val owner: User? = null,
	@ManyToOne
	val ownerGroup: Group? = null,
	@Deprecated("Dynamically generated using Jump::location")
	var image: String? = null,
	@OneToOne
	val meta: Meta,
	var usage: Int = 0
) {
	companion object {
		const val TYPE_GLOBAL: Int = 0
		const val TYPE_PERSONAL: Int = 1
		const val TYPE_GROUP: Int = 2

	}
	fun isPublic(): Boolean = owner == null && ownerGroup == null

	/**
	 * Check whether the Jump ID matches [other]
	 * Otherwise fallback to the superclass implementation
	 */
	override fun equals(other: Any?): Boolean {
		if(other is Jump)
			return other.id == id && other.name == name
		return super.equals(other)
	}
}
