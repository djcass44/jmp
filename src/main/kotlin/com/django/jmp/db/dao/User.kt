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

object Users: IntIdTable() {
    val username = varchar("username", 36).uniqueIndex()
    val hash = text("hash")
    val token = uuid("token")
    val role = reference("role", Roles)
    val requestToken = text("request_token").nullable()
}
class User(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var username by Users.username
    var hash by Users.hash
    var token by Users.token
    var role by Role referencedOn Users.role
    var requestToken by Users.requestToken
}
data class UserData(val id: Int, val username: String, val role: String, val groups: ArrayList<String>) {
    constructor(user: User, groups: ArrayList<String>): this(user.id.value, user.username, user.role.name, groups)
}
data class EditUserData(val id: Int, val role: String)