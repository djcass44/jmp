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

object Jumps : IntIdTable() {
    val name = varchar("name", 50)
    val location = varchar("location", 2083)
    val owner = optReference("owner", Users)
    val ownerGroup = optReference("ownerGroup", Groups)
    val image = varchar("image", 2083).nullable()
}

class Jump(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Jump>(Jumps)

    var name by Jumps.name
    var location by Jumps.location
    var owner by User optionalReferencedOn Jumps.owner
    var ownerGroup by Group optionalReferencedOn Jumps.ownerGroup
    var image by Jumps.image
}
data class JumpData(val id: Int, val name: String, val location: String, val personal: Boolean = false, val image: String? = null) {
    constructor(jump: Jump): this(jump.id.value, jump.name, jump.location, jump.owner != null || jump.ownerGroup != null, jump.image)
}
data class EditJumpData(val id: Int, val name: String, val location: String)