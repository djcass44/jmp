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
import org.jetbrains.exposed.sql.transactions.transaction

object Jumps : IntIdTable() {
    val name = varchar("name", 50)
    val location = varchar("location", 2083)
    val owner = optReference("owner", Users)
    val ownerGroup = optReference("ownerGroup", Groups)
    val image = varchar("image", 2083).nullable()

    val metaCreation = long("metaCreation").default(0)
    val metaUpdate = long("metaUpdate").default(0)
    val metaUsage = integer("metaUsage").default(0)
}

class Jump(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Jump>(Jumps)

    var name by Jumps.name
    var location by Jumps.location
    var owner by User optionalReferencedOn Jumps.owner
    var ownerGroup by Group optionalReferencedOn Jumps.ownerGroup
    var image by Jumps.image

    var metaCreation by Jumps.metaCreation
    var metaUpdate by Jumps.metaUpdate
    var metaUsage by Jumps.metaUsage
}
data class JumpData(val id: Int, val name: String, val location: String, val personal: Int = 0, val owner: String? = null, val image: String? = null) {
    constructor(jump: Jump): this(jump.id.value, jump.name, jump.location, getType(jump), getOwner(jump), jump.image)

    companion object {
        const val TYPE_GLOBAL = 0
        const val TYPE_PERSONAL = 1
        const val TYPE_GROUP = 2

        fun getType(jump: Jump): Int {
            if(jump.ownerGroup == null && jump.owner == null)
                return TYPE_GLOBAL
            if(jump.owner != null)
                return TYPE_PERSONAL
            if(jump.ownerGroup != null)
                return TYPE_GROUP
            return TYPE_GLOBAL
        }
        fun getOwner(jump: Jump): String? = transaction {
            if(jump.ownerGroup == null && jump.owner == null)
                return@transaction null
            if(jump.ownerGroup != null)
                return@transaction jump.ownerGroup!!.name
            if(jump.owner != null)
                return@transaction jump.owner!!.username
            return@transaction null
        }
    }
}
data class EditJumpData(val id: Int, val name: String, val location: String)