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

package dev.castive.jmp.api.v1

import dev.castive.javalin_auth.actions.UserAction
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.actions.ImageAction
import dev.castive.jmp.api.actions.OwnerAction
import dev.castive.jmp.api.v2_1.WebSocket
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.db.Util
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.dao.Jump
import dev.castive.jmp.except.EmptyPathException
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.ConflictResponse
import io.javalin.ForbiddenResponse
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class Jump(private val config: ConfigStore, private val ws: WebSocket): EndpointGroup {
    private fun jumpExists(name: String, location: String, user: User?): Boolean {
        return transaction {
            /**
             * Get Jumps for user
             * Return TRUE if both name & location matches any
             */
            val existing = OwnerAction.getJumpFromUser(user, name)
            existing.forEach {
                if(it.name == name && it.location == location)
                    return@transaction true
            }
            return@transaction false
        }
    }

    override fun addEndpoints() {
        // List all items in Json format
        get("${Runner.BASE}/v1/jumps", { ctx ->
            val items = arrayListOf<JumpData>()
            val user = ClaimConverter.getUser(UserAction.getOrNull(ctx))
            val userJumps = OwnerAction.getUserVisibleJumps(user)
            transaction {
                userJumps.forEach {
                    items.add(JumpData(it))
                }
            }
            ctx.json(items).status(HttpStatus.OK_200)
        }, Auth.openAccessRole)
        get("${Runner.BASE}/v2/jump/:target", { ctx ->
            try {
                val target = ctx.pathParam("target")
                if(target.isBlank())
                    throw EmptyPathException()
                /**
                 * 1. Try to get JWT token
                 */
                val user = ClaimConverter.getUser(UserAction.getOrNull(ctx))
                transaction {
                    Log.d(javaClass, "User information: [name: ${user?.username}, token: ${user?.id?.value}]")
                    Log.d(javaClass, "Found user: ${user != null}")
                    val jump = OwnerAction.getJumpFromUser(user, target)
                    if(jump.size == 1) {
                        val location = jump[0].location
                        jump[0].metaUsage++ // Increment usage count for statistics
                        Log.v(javaClass, "v2: moving to point: $location")
                        ctx.status(HttpStatus.OK_200).result(location) // Send the user the result, don't redirect them
                    }
                    else ctx.status(HttpStatus.OK_200).result("${config.BASE_URL}/similar?query=$target")
                }
            }
            catch (e: IndexOutOfBoundsException) {
                Log.e(javaClass, "Invalid target: ${ctx.path()}")
                throw BadRequestResponse()
            }
            catch (e: EmptyPathException) {
                Log.e(javaClass, "Empty target")
                throw NotFoundResponse()
            }
        }, Auth.openAccessRole)
        // Add a jump point
        put("${Runner.BASE}/v1/jump", { ctx ->
            val add = ctx.bodyAsClass(JumpData::class.java)
            val groupID = Util.getSafeUUID(ctx.queryParam("gid") ?: "")
            val user = ClaimConverter.getUser(UserAction.get(ctx))!!
            // Block non-admin user from adding global jumps
            if (add.personal == JumpData.TYPE_GLOBAL && transaction { return@transaction user.role.name != Auth.BasicRoles.ADMIN.name }) throw ForbiddenResponse()
            if (!jumpExists(add.name, add.location, user)) {
                transaction {
                    val group = if(groupID != null) Group.findById(groupID) else null
                    val jump = Jump.new {
                        name = add.name
                        location = add.location
                        owner = if (add.personal == JumpData.TYPE_PERSONAL) user else null
                        ownerGroup = group
                        metaCreation = System.currentTimeMillis()
                        metaUpdate = System.currentTimeMillis()
                    }
                    // Create aliases for newly added Jump
                    add.alias.forEach { Alias.new {
                        name = it.name
                        parent = jump
                    } }
                    ImageAction(add.location).get()
                }
                ws.fire(WebSocket.EVENT_UPDATE, WebSocket.EVENT_UPDATE)
                ctx.status(HttpStatus.CREATED_201).json(add)
            }
            else throw ConflictResponse()
        }, Auth.openAccessRole)
        // Edit a jump point
        patch("${Runner.BASE}/v1/jump", { ctx ->
            val update = ctx.bodyAsClass(EditJumpData::class.java)
            val user = ClaimConverter.getUser(UserAction.get(ctx))!!
//            if(jumpExists(update.name, update.location, user)) throw ConflictResponse()
            transaction {
                val existing = Jump.findById(update.id) ?: throw NotFoundResponse()

                // User can change personal jumps
                if(existing.owner == user || user.role.name == Auth.BasicRoles.ADMIN.name) {
                    existing.apply {
                        name = update.name
                        location = update.location
                        metaUpdate = System.currentTimeMillis()
                    }
                    // Add aliases
                    update.alias.forEach {
                        val alias = Alias.findById(it.id)
                        if(it.id == 0 || alias == null) {
                            // Create the alias if it doesn't exist
                            Alias.new {
                                name = it.name
                                parent = existing
                            }
                        }
                        // Update the alias name (needs investigation into ability to abuse)
                        else alias.name = it.name
                    }
                    ImageAction(update.location).get()
                    ws.fire(WebSocket.EVENT_UPDATE, WebSocket.EVENT_UPDATE)
                    ctx.status(HttpStatus.NO_CONTENT_204).json(update)
                }
                else throw ForbiddenResponse()
            }
        }, Auth.defaultRoleAccess)
        // Delete a jump point
        delete("${Runner.BASE}/v1/jump/:id", { ctx ->
            val id = ctx.pathParam("id").toIntOrNull() ?: throw BadRequestResponse()
            val user = ClaimConverter.getUser(UserAction.get(ctx))!!
            transaction {
                val result = Jump.findById(id) ?: throw NotFoundResponse()
                // 403 if jump is global and user ISN'T admin
                if (result.owner == null && user.role.name != Auth.BasicRoles.ADMIN.name) throw ForbiddenResponse()
                // 403 if jump is personal and tokens don't match
                if (result.owner != null && result.owner!!.id != user.id) throw ForbiddenResponse()
                // Delete all aliased children so that they don't become orphans
                Alias.find { Aliases.parent eq result.id }.forEach { it.delete() }
                result.delete()
            }
            ws.fire(WebSocket.EVENT_UPDATE, WebSocket.EVENT_UPDATE)
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, Auth.defaultRoleAccess)
    }
}
