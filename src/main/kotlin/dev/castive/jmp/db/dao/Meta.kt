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

package dev.castive.jmp.db.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable

object Metas: LongIdTable() {
	val created = long("created").default(System.currentTimeMillis())
	val edited = long("edited").default(System.currentTimeMillis())

	val createdBy = reference("createdBy", Users)
	val editedBy = reference("editedBy", Users)
}
class Meta(id: EntityID<Long>): LongEntity(id) {
	companion object: LongEntityClass<Meta>(Metas)

	var created by Metas.created
	var edited by Metas.edited

	var createdBy by User referencedOn Metas.createdBy
	var editedBy by User referencedOn Metas.editedBy

	fun onEdit(user: User) {
		edited = System.currentTimeMillis()
		editedBy = user
	}
}
data class MetaEntity(
	val id: Long,
	val created: Long,
	val edited: Long,
	val createdBy: SafeUserEntity,
	val editedBy: SafeUserEntity
) {
	constructor(meta: Meta): this(meta.id.value, meta.created, meta.edited, SafeUserEntity(meta.createdBy), SafeUserEntity(meta.editedBy))
}
