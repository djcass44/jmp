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

import dev.castive.jmp.db.repo.findAllByParent
import dev.castive.jmp.util.asArrayList
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction

object Jumps : IntIdTable() {
    val name = varchar("name", 50)
    val location = varchar("location", 2083)
    val title = text("title").nullable()
    val owner = optReference("owner", Users)
    val ownerGroup = optReference("ownerGroup", Groups)
    val image = varchar("image", 2083).nullable()

    val metaCreation = long("metaCreation").default(System.currentTimeMillis())
    val metaUpdate = long("metaUpdate").default(System.currentTimeMillis())
    val metaUsage = integer("metaUsage").default(0)

	val meta = optReference("meta", Metas)
}

class Jump(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Jump>(Jumps)

    var name by Jumps.name
    var location by Jumps.location
    var title by Jumps.title
    var owner by User optionalReferencedOn Jumps.owner
    var ownerGroup by Group optionalReferencedOn Jumps.ownerGroup
    var image by Jumps.image

    var metaCreation by Jumps.metaCreation
    var metaUpdate by Jumps.metaUpdate
    var metaUsage by Jumps.metaUsage

	var meta by Meta optionalReferencedOn Jumps.meta

    fun isPublic(): Boolean = transaction {
        owner == null && ownerGroup == null
    }
}
data class JumpData(val id: Int, val name: String, val location: String, val personal: Int = 0, val owner: String? = null, val image: String? = null,
                    val title: String? = null,
                    val alias: ArrayList<AliasData>,
                    val metaCreation: Long = 0,
                    val metaUpdate: Long = 0,
                    val metaUsage: Int = 0,
                    val meta: MetaEntity? = null) {
    constructor(jump: Jump): this(jump.id.value,
	    jump.name,
	    jump.location,
	    getType(jump),
	    getOwner(jump),
	    jump.image,
	    jump.title,
	    getAlias(jump),
	    jump.metaCreation,
	    jump.metaUpdate,
	    jump.metaUsage,
	    jump.meta?.let { MetaEntity(it) }
    )

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
        fun getOwner(jump: Jump): String? = transaction {
	        when {
		        jump.ownerGroup != null -> jump.ownerGroup!!.name
		        jump.owner != null -> jump.owner!!.username
		        else -> null
	        }
        }
        fun getAlias(jump: Jump): ArrayList<AliasData> = transaction {
            // Get all the aliases which are owned by @param jump
            Aliases.findAllByParent(jump.id.value).map { AliasData(it) }.asArrayList()
        }
    }
}
data class EditJumpData(val id: Int, val name: String, val alias: ArrayList<AliasData>, val location: String)
