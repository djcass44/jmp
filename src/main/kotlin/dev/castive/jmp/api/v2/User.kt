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

package dev.castive.jmp.api.v2

import com.google.common.util.concurrent.RateLimiter
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.Responses
import dev.castive.jmp.api.Socket
import dev.castive.jmp.auth.ConfigBuilder
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.repo.count
import dev.castive.jmp.db.repo.findAllByName
import dev.castive.jmp.db.repo.findAllContainingUser
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.asArrayList
import dev.castive.jmp.util.assertUser
import dev.castive.jmp.util.ok
import dev.castive.jmp.util.user
import dev.castive.log2.Log
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.ForbiddenResponse
import io.javalin.http.UnauthorizedResponse
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.math.ceil

class User(
    private val auth: Auth,
    private val ws: (tag: String, data: Any) -> (Unit),
    private val config: ConfigBuilder.JMPConfiguration
): EndpointGroup {
    private val createLimiter = RateLimiter.create(5.0)

    override fun addEndpoints() {
        get("${Runner.BASE}/v2/users", { ctx ->
            ctx.assertUser()
            Log.d(javaClass, "list - JWT validation passed")
            val count = ctx.queryParam<Int>("count").value?.coerceAtLeast(5) ?: 10
            val offset = ctx.queryParam<Int>("offset").value?.coerceAtLeast(0) ?: 0
            Log.d(javaClass, "/v2/users [count: $count, offset: $offset]")
            val users = transaction {
                // get the users memberships and map them to the user entity
                User.all().limit(count, offset).map {
                    UserData(it, Groups.findAllContainingUser(it).map { g -> g.name }.asArrayList())
                }.asArrayList()
            }
            val userCount = Users.count()
            val currentPage = (offset / userCount) + 1
            val totalPages = ceil(userCount / count.toDouble()).toInt()
            Log.d(javaClass, "Returning ${users.size} users")
            ctx.ok().json(PagedUserData(currentPage, totalPages, users, offset + (count * 2) < userCount))
        }, Auth.defaultRoleAccess)
        // Add a user
        put("${Runner.BASE}/v2/user", { ctx ->
            if(createLimiter.tryAcquire()) {
                val user: User? = ctx.user()
                val blockLocal = config.blockLocal
                Log.d(javaClass, "Block local accounts: $blockLocal")
                if ((user == null || !auth.isAdmin(user)) && blockLocal) {
                    Log.i(javaClass, "User ${user?.username} is not allowed to create local accounts [reason: POLICY]")
                    throw UnauthorizedResponse("Creating local accounts has been disabled.")
                }
                val basicAuth = ctx.basicAuthCredentials()
                Log.i(javaClass, "$user is creating a user [name: ${basicAuth.username}]")
                auth.createUser(basicAuth.username, basicAuth.password.toCharArray())
                // ask the groupstask cron to update public/default relations
                GroupsTask.update()
                ws.invoke(Socket.EVENT_UPDATE_USER, Socket.EVENT_UPDATE_USER)
                ctx.status(HttpStatus.CREATED_201).result(basicAuth.username)
            }
            else
                ctx.status(HttpStatus.TOO_MANY_REQUESTS_429).result(Responses.GENERIC_RATE_LIMITED)
        }, Auth.openAccessRole)
        // Get information about the current user
        get("${Runner.BASE}/v2/user", { ctx ->
            val user = ctx.assertUser()
	        transaction {
		        ctx.ok().json(UserData(user, arrayListOf()))
	        }
        }, Auth.defaultRoleAccess)
        // Change the role of a user
        patch("${Runner.BASE}/v2/user", { ctx ->
            val updated = ctx.bodyAsClass(EditUserData::class.java)
            val u = ctx.assertUser()
            transaction {
                val user = User.findById(updated.id) ?: throw BadRequestResponse()
                // Block dropping the superuser from admin
                if(user.username == "admin") throw ForbiddenResponse()
                // Block the user from changing their own permissions
                if(user.username == u.username) throw ForbiddenResponse()
                val role = Roles.findAllByName(updated.role).elementAtOrNull(0) ?: throw BadRequestResponse()
                Log.i(javaClass, "User role updated [user: ${user.username}, from: ${user.role.name}, to: ${role.name}] by ${u.username}")
                user.apply {
                    this.role = role
                    metaUpdate = System.currentTimeMillis()
                }
                ws.invoke(Socket.EVENT_UPDATE_USER, Socket.EVENT_UPDATE_USER)
                ctx.status(HttpStatus.NO_CONTENT_204).json(updated)
            }
        }, Auth.adminRoleAccess)
        // Delete a user
        delete("${Runner.BASE}/v2/user/:id", { ctx ->
            val id = UUID.fromString(ctx.pathParam("id"))
            val user = ctx.assertUser()
            transaction {
                val target = User.findById(id) ?: throw BadRequestResponse()
                Log.i(javaClass, "[${user.username}] is removing ${target.username}")
                if(target.username == "admin") {
                    Log.w(javaClass, "[${user.username}] is attempting to remove the superuser")
                    throw ForbiddenResponse()
                } // Block deleting the superuser
                if(target.username == user.username) {
                    Log.w(javaClass, "[${user.username}] is attempting to remove themselves")
                    throw ForbiddenResponse()
                } // Stop the users deleting themselves
                target.delete()
            }
            ws.invoke(Socket.EVENT_UPDATE_USER, Socket.EVENT_UPDATE_USER)
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, Auth.adminRoleAccess)
        get("${Runner.BASE}/v2_1/user/groups", { ctx ->
            val uid = runCatching {
                UUID.fromString(ctx.queryParam("uid"))
            }.getOrElse {
                throw BadRequestResponse("Bad UUID")
            }
            val items = transaction {
                val getUser = User.findById(uid) ?: throw BadRequestResponse("Requested user is null")
                return@transaction Groups.findAllContainingUser(getUser).map { GroupData(it) }
            }
            ctx.ok().json(items)
        }, Auth.defaultRoleAccess)
    }
}
