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

package com.django.jmp.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Jumps : IntIdTable() {
    val name = varchar("name", 50)
    val location = varchar("location", 2083)
    val owner = reference("owner", Users).nullable()
    val image = varchar("image", 2083).nullable()
}

class Jump(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Jump>(Jumps)

    var name by Jumps.name
    var location by Jumps.location
    var owner by User optionalReferencedOn Jumps.owner
    var image by Jumps.image
}
data class JumpData(val id: Int, val name: String, val location: String, val personal: Boolean = false, val image: String? = null) {
    constructor(jump: Jump): this(jump.id.value, jump.name, jump.location, jump.owner != null, jump.image)
}
data class EditJumpData(val id: Int, val name: String, val location: String)

object Users: IntIdTable() {
    val username = varchar("username", 36).uniqueIndex()
    val hash = text("hash")
    val token = uuid("token")
    val role = reference("role", Roles)
}
class User(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var hash by Users.hash
    var token by Users.token
    var role by Role referencedOn Users.role
}
data class UserData(val id: Int, val username: String, val role: String) {
    constructor(user: User): this(user.id.value, user.username, user.role.name)
}
data class EditUserData(val id: Int, val role: String)

object Roles: IntIdTable() {
    val name = varchar("name", 12).uniqueIndex()
}
class Role(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Role>(Roles)

    var name by Roles.name
}