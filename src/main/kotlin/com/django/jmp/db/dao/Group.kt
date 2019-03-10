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

package com.django.jmp.db.dao

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table

object Groups: IntIdTable() {
    val name = varchar("name", 24).uniqueIndex()
}
class Group(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Group>(Groups)

    var name by Groups.name
    var users by User via GroupUsers
}
data class GroupData(val id: Int, val name: String) {
    constructor(group: Group): this(group.id.value, group.name)
}
object GroupUsers: Table() {
    val group = reference("group", Groups).primaryKey(0)
    val users = reference("users", Users).primaryKey(1)
}