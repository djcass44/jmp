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

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Aliases: IntIdTable() {
    val name = varchar("name", 50)
    val parent = reference("jump", Jumps)
}
class Alias(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Alias>(Aliases)

    var name by Aliases.name
    var parent by Jump referencedOn Aliases.parent
}
data class AliasData(val id: Int, val name: String) {
    constructor(alias: Alias): this(alias.id.value, alias.name)
}