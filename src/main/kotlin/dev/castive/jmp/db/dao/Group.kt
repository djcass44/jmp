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

package dev.castive.jmp.db.dao

import dev.castive.javalin_auth.auth.provider.InternalProvider
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Table
import java.util.*

object Groups: UUIDTable() {
    val name = varchar("name", 255).uniqueIndex()

    val from = varchar("from", 24).default(InternalProvider.SOURCE_NAME)
    // whether the group should be publicly visible (only allowed for internal groups)
    val public = bool("public").default(false)
    // users from this source will be automatically added to this group
    val defaultFor = varchar("default_for", 24).nullable()
}
class Group(id: EntityID<UUID>): UUIDEntity(id) {
    companion object: UUIDEntityClass<Group>(Groups)

    var name by Groups.name
    var users by User via GroupUsers

    var from by Groups.from
    var public by Groups.public
    var defaultFor by Groups.defaultFor
}
data class GroupData(
    val id: UUID? = UUID.randomUUID(),
    val name: String,
    val from: String = InternalProvider.SOURCE_NAME,
    val public: Boolean = false,
    val defaultFor: String? = null
) {
    constructor(group: Group): this(group.id.value, group.name, group.from, group.public, group.defaultFor)
}
object GroupUsers: Table() {
    val group = reference("group", Groups).primaryKey(0)
    val users = reference("users", Users).primaryKey(1)
}
