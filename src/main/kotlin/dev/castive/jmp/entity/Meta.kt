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

import java.util.UUID
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Meta(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long,
	val created: Long = System.currentTimeMillis(),
	var edited: Long = System.currentTimeMillis(),
	val createdBy: UUID,
	var editedBy: UUID
) {
	companion object {
		fun fromUser(id: UUID): Meta = Meta(0, createdBy = id, editedBy = id)
		fun fromUser(user: User): Meta = Meta(0, createdBy = user.id, editedBy = user.id)
	}

	fun onUpdate(user: User) = apply {
		edited = System.currentTimeMillis()
		editedBy = user.id
	}
}
