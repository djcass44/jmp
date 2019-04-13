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

package dev.castive.jmp.api.v2_1

import dev.castive.log2.Log
import dev.castive.jmp.api.Auth
import dev.castive.jmp.Runner
import dev.castive.jmp.api.actions.UserAction
import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.User
import io.javalin.BadRequestResponse
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.EndpointGroup
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.ArrayList

class GroupMod: EndpointGroup {
    data class GroupModPayload(val add: ArrayList<String>, val rm: ArrayList<String>)

    override fun addEndpoints() {
        patch("${Runner.BASE}/v2_1/groupmod", { ctx ->
            val addUser = UUID.fromString(ctx.queryParam("uid")) ?: throw BadRequestResponse("UID cannot be null")
            val mods = ctx.bodyAsClass(GroupModPayload::class.java)

            Log.d(javaClass, "add - queryParams valid")
            val user = UserAction.get(ctx)
            Log.d(javaClass, "add - JWT validation passed")
            transaction {
                val newUser = User.findById(addUser) ?: throw NotFoundResponse("Invalid uid")
                // Add user to groups
                for (g in mods.add) {
                    val group = Group.findById(UUID.fromString(g)) ?: throw NotFoundResponse("Invalid gid: $g")
                    if(user.role.name == Auth.BasicRoles.ADMIN.name || group.users.contains(user)) {
                        // Add user to GroupUsers
                        val newUsers = ArrayList<User>()
                        newUsers.addAll(group.users)
                        newUsers.add(newUser)
                        group.users = SizedCollection(newUsers)
                        Log.i(javaClass, "${user.username} added ${newUser.username} to group ${group.name}")
                    }
                }
                for (g in mods.rm) {
                    val group = Group.findById(UUID.fromString(g)) ?: throw NotFoundResponse("Invalid gid: $g")
                    if(user.role.name == Auth.BasicRoles.ADMIN.name || group.users.contains(user)) {
                        // Remove user from GroupUsers
                        val newUsers = ArrayList<User>()
                        newUsers.addAll(group.users)
                        newUsers.remove(newUser)
                        group.users = SizedCollection(newUsers)
                        Log.i(javaClass, "${user.username} removed ${newUser.username} from group ${group.name}")
                    }
                }
            }
        }, Auth.defaultRoleAccess)
    }
}