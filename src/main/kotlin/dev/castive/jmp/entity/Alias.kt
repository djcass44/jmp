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

import dev.castive.jmp.data.dto.AliasDTO
import dev.castive.log2.logv
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import javax.persistence.*

@Indexed
@Entity
@Table(name = "Aliases")
data class Alias(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Int,
	@Field
	var name: String,
	val parent: Int
) {

	@PreRemove
	fun onPreRemove() {
		"Removing ${javaClass.simpleName} with id: $id".logv(javaClass)
	}

	fun asDTO(): AliasDTO = AliasDTO(id, name)

	/**
	 * Check whether the Alias ID matches [other]
	 * Otherwise fallback to the superclass implementation
	 */
	override fun equals(other: Any?): Boolean {
		if(other is Alias)
			return other.id == id
		return super.equals(other)
	}
}
