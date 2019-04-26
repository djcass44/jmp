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
import java.util.*

object Users: UUIDTable() {
    val username = varchar("username", 36).uniqueIndex()
    val hash = text("hash")
    val role = reference("role", Roles)
    val requestToken = text("request_token").nullable()

    val metaCreation = long("metaCreation").default(System.currentTimeMillis())
    val metaUpdate = long("metaUpdate").default(System.currentTimeMillis())

    val from = varchar("from", 24).default(InternalProvider.SOURCE_NAME)
}
class User(id: EntityID<UUID>): UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var username by Users.username
    var hash by Users.hash
    var role by Role referencedOn Users.role
    var requestToken by Users.requestToken

    var metaCreation by Users.metaCreation
    var metaUpdate by Users.metaUpdate

    var from by Users.from
}
data class UserData(val id: UUID, val username: String, val role: String, val groups: ArrayList<String>, val metaCreation: Long = 0, val metaUpdate: Long = 0, val from: String = InternalProvider.SOURCE_NAME) {
    constructor(user: User, groups: ArrayList<String>): this(user.id.value, user.username, user.role.name, groups, user.metaCreation, user.metaUpdate, user.from)
    constructor(user: User): this(user, arrayListOf())
}
data class PagedUserData(val currentPage: Int, val totalPages: Int, val users: ArrayList<UserData>, val next: Boolean)
data class EditUserData(val id: UUID, val role: String)