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

import dev.castive.javalin_auth.auth.Roles.BasicRoles
import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.Socket
import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.GroupData
import dev.castive.jmp.db.dao.GroupUsers
import dev.castive.jmp.db.dao.Groups
import dev.castive.jmp.db.repo.findAllByName
import dev.castive.jmp.db.repo.findAllContainingUser
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.assertUser
import dev.castive.jmp.util.isEqual
import dev.castive.jmp.util.ok
import dev.castive.jmp.util.user
import dev.castive.log2.Log
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.ConflictResponse
import io.javalin.http.ForbiddenResponse
import io.javalin.http.NotFoundResponse
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Group(private val ws: (tag: String, data: Any) -> (Unit)): EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2_1/groups", { ctx ->
            val items = arrayListOf<GroupData>()
            val user = ctx.user()
            Log.d(javaClass, "Listing groups visible to actual user: ${user?.username}")
            if(user != null) {
                transaction {
                    if(user.role.isEqual(BasicRoles.ADMIN)) {
                        items.addAll(Group.all().map { GroupData(it) })
                        return@transaction
                    }
                    items.addAll(Groups.findAllContainingUser(user).map { GroupData(it) })
                }
            }
            ctx.ok().json(items)
        }, Auth.defaultRoleAccess)
        put("${Runner.BASE}/v2_1/group", { ctx ->
            val add = ctx.bodyAsClass(GroupData::class.java)
            val user = ctx.assertUser()
            transaction {
                val existing = Groups.findAllByName(add.name)
                if(existing.isNotEmpty()) throw ConflictResponse("Group already exists")
                Group.new(UUID.randomUUID()) {
                    name = add.name
                    public = add.public
                    if(!public)
                        defaultFor = add.defaultFor
                    // Add the user to the new group
                    users = SizedCollection(arrayListOf(user))
                }
            }
            ws.invoke(Socket.EVENT_UPDATE_GROUP, Socket.EVENT_UPDATE_GROUP)
            ctx.status(HttpStatus.CREATED_201).json(add)
        }, Auth.defaultRoleAccess)
        patch("${Runner.BASE}/v2_1/group", { ctx ->
            val update = ctx.bodyAsClass(GroupData::class.java)
            val user = ctx.assertUser()
            transaction {
                val existing = Group.findById(update.id!!) ?: throw NotFoundResponse("Group not found")
                // Only allow update if user belongs to group (or is admin)
                if(!user.role.isEqual(BasicRoles.ADMIN) && !existing.users.contains(user)) throw ForbiddenResponse("User not in requested group")
                // update the group
                existing.apply {
                    name = update.name
                    // only allow the public flag to be changed for internal groups
                    if(from == InternalProvider.SOURCE_NAME && user.role.isEqual(BasicRoles.ADMIN)) {
                        public = update.public
                        // we cannot have a public and default group
                        if(!public)
                            defaultFor = update.defaultFor
                    }
                }
            }
            // ask the groupstask cron to update public/default relations
            GroupsTask.update()
            ws.invoke(Socket.EVENT_UPDATE_GROUP, Socket.EVENT_UPDATE_GROUP)
            ctx.status(HttpStatus.NO_CONTENT_204).json(update)
        }, Auth.defaultRoleAccess)
        delete("${Runner.BASE}/v2_1/group/:id", { ctx ->
            val id = UUID.fromString(ctx.pathParam("id"))
            val user = ctx.assertUser()
            transaction {
                val existing = Group.findById(id) ?: throw NotFoundResponse("Group not found")
                // Only allow deletion if user belongs to group (or is admin)
                if(!user.role.isEqual(BasicRoles.ADMIN) && !existing.users.contains(user)) throw ForbiddenResponse("User not in requested group")
                Log.i(javaClass, "${user.username} is removing group ${existing.name}")
                // cleanup the intermediate tables
                GroupUsers.deleteWhere {
                    GroupUsers.group eq existing.id
                }
                existing.delete()
            }
            ws.invoke(Socket.EVENT_UPDATE_GROUP, Socket.EVENT_UPDATE_GROUP)
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, Auth.adminRoleAccess)
    }
}
