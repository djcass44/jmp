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

import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.actions.UserAction
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.dao.Group
import dev.castive.log2.Log
import io.javalin.ConflictResponse
import io.javalin.ForbiddenResponse
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Group(private val ws: WebSocket): EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2_1/groups", { ctx ->
            val items = arrayListOf<GroupData>()
            val user = UserAction.getOrNull(ctx)
            if(user != null) {
                transaction {
                    if(user.role.name == dev.castive.jmp.api.Auth.BasicRoles.ADMIN.name) {
                        Group.all().forEach {
                            items.add(GroupData((it)))
                        }
                        return@transaction
                    }
                    val res = (Groups innerJoin GroupUsers innerJoin Users)
                        .slice(Groups.columns)
                        .select {
                            Users.id eq user.id
                        }
                        .withDistinct()
                    val groups = Group.wrapRows(res).toList()
                    groups.forEach {
                        items.add(GroupData(it))
                    }
                }
            }
            ctx.status(HttpStatus.OK_200).json(items)
        }, Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2_1/group/:id", { ctx ->
            // Only allow users to view groups they're already in
            ctx.status(HttpStatus.FORBIDDEN_403).result("This endpoint is unfinished, or not ready for public use.")
        }, Auth.defaultRoleAccess)
        put("${Runner.BASE}/v2_1/group", { ctx ->
            val add = ctx.bodyAsClass(GroupData::class.java)
            val user = UserAction.get(ctx)
            Log.d(javaClass, "add - JWT validation passed")
            transaction {
                val existing = Group.find {
                    Groups.name eq add.name
                }
                if(existing.count() > 0) throw ConflictResponse("Group already exists")
                Group.new(UUID.randomUUID()) {
                    name = add.name
                    // Add the user to the new group
                    users = SizedCollection(arrayListOf(user))
                }
            }
            ws.fire(WebSocket.EVENT_UPDATE_GROUP, WebSocket.EVENT_UPDATE_GROUP)
            ctx.status(HttpStatus.CREATED_201).json(add)
        }, Auth.defaultRoleAccess)
        patch("${Runner.BASE}/v2_1/group", { ctx ->
            val update = ctx.bodyAsClass(GroupData::class.java)
            val user = UserAction.get(ctx)
            transaction {
                val existing = Group.findById(update.id!!) ?: throw NotFoundResponse("Group not found")
                // Only allow update if user belongs to group (or is admin)
                if(user.role.name != dev.castive.jmp.api.Auth.BasicRoles.ADMIN.name && !existing.users.contains(user)) throw ForbiddenResponse("User not in requested group")
                existing.name = update.name
                ws.fire(WebSocket.EVENT_UPDATE_GROUP, WebSocket.EVENT_UPDATE_GROUP)
                ctx.status(HttpStatus.NO_CONTENT_204).json(update)
            }
        }, Auth.defaultRoleAccess)
        delete("${Runner.BASE}/v2_1/group/:id", { ctx ->
            val id = UUID.fromString(ctx.pathParam("id"))
            val user = UserAction.get(ctx)
            transaction {
                val existing = Group.findById(id) ?: throw NotFoundResponse("Group not found")
                // Only allow deletion if user belongs to group (or is admin)
                if(user.role.name != dev.castive.jmp.api.Auth.BasicRoles.ADMIN.name && !existing.users.contains(user)) throw ForbiddenResponse("User not in requested group")
                Log.i(javaClass, "${user.username} is removing group ${existing.name}")
                GroupUsers.deleteWhere {
                    GroupUsers.group eq existing.id
                }
                existing.delete()
                ws.fire(WebSocket.EVENT_UPDATE_GROUP, WebSocket.EVENT_UPDATE_GROUP)
                ctx.status(HttpStatus.NO_CONTENT_204)
            }
        }, Auth.adminRoleAccess)
    }
}