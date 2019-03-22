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

package com.django.jmp.api.v1

import com.django.jmp.api.Auth
import com.django.jmp.api.Runner
import com.django.jmp.api.actions.ImageAction
import com.django.jmp.api.actions.OwnerAction
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.auth.response.AuthenticateResponse
import com.django.jmp.db.ConfigStore
import com.django.jmp.db.dao.*
import com.django.jmp.db.dao.Jump
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.ConflictResponse
import io.javalin.ForbiddenResponse
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Jump(private val auth: Auth, private val config: ConfigStore): EndpointGroup {
    private fun jumpExists(name: String): Boolean {
        return transaction {
            val existing = Jump.find {
                Jumps.name.lowerCase() eq name.toLowerCase() and Jumps.owner.isNull()
            }
            return@transaction !existing.empty()
        }
    }
    private fun jumpExists(name: String, user: String?, token: UUID?): Boolean {
        if(token == null)
            return jumpExists(name) // Fall back to v1 without a token
        return transaction {
            val userObject = auth.getUser(user, token)
            val existing = OwnerAction.getInstance().getJumpFromUser(userObject, name)
            return@transaction if(existing == null) jumpExists(name) else true // Fall back to v1 if nothing found in v2
        }
    }

    override fun addEndpoints() {
        // List all items in Json format
        get("${Runner.BASE}/v1/jumps", { ctx ->
            val items = arrayListOf<JumpData>()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: ""
            val user = if(jwt == "null" || jwt.isBlank()) null else TokenProvider.getInstance().verify(jwt)
            val userJumps = OwnerAction.getInstance().getUserVisibleJumps(user)
            transaction {
                userJumps.forEach {
                    items.add(JumpData(it))
                }
            }
            ctx.json(items).status(HttpStatus.OK_200)
        }, Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2/jump/:target", { ctx ->
            try {
                val target = ctx.pathParam("target")
                if(target.isBlank())
                    throw EmptyPathException()
                /**
                 * 1. Try to get JWT token
                 * 2. Assume no token, redirect to checker
                 */
                val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: ""
                if (jwt.isBlank()) {
                    Log.d(javaClass, "User has no token, redirecting for check...")
                    ctx.status(HttpStatus.FOUND_302).redirect("${config.BASE_URL}/jmp?query=$target")
                    return@get
                }
                val user = if(jwt.isBlank()) null else TokenProvider.getInstance().verify(jwt)
                transaction {
                    Log.d(javaClass, "User information: [name: ${user?.username}, token: ${user?.token}]")
                    Log.d(javaClass, "Found user: ${user != null}")
                    val jump = OwnerAction.getInstance().getJumpFromUser(user, target)
                    if(jump != null) {
                        val location = jump.location
                        jump.metaUsage++ // Increment usage count for statistics
                        Log.v(javaClass, "v2: moving to point: $location")
                        ctx.status(HttpStatus.OK_200).result(location) // Send the user the result, don't redirect them
                    }
                    else ctx.status(HttpStatus.OK_200).result("${config.BASE_URL}/similar?query=$target")
                }
            }
            catch (e: IndexOutOfBoundsException) {
                Log.e(Runner::class.java, "Invalid target: ${ctx.path()}")
                throw BadRequestResponse()
            }
            catch (e: EmptyPathException) {
                Log.e(Runner::class.java, "Empty target")
                throw NotFoundResponse()
            }
        }, Auth.defaultRoleAccess)
        // Add a jump point
        put("${Runner.BASE}/v1/jumps/add", { ctx ->
            val add = ctx.bodyAsClass(JumpData::class.java)
            val groupID = kotlin.runCatching { UUID.fromString(ctx.queryParam("gid")) }.getOrNull()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val user = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            // Block non-admin user from adding global jumps
            if (add.personal == JumpData.TYPE_GLOBAL && transaction { return@transaction user.role.name != Auth.BasicRoles.ADMIN.name }) throw ForbiddenResponse()
            if (!jumpExists(add.name, user.username, user.token)) {
                transaction {
                    val group = if(groupID != null) Group.findById(groupID) else null
                    Jump.new {
                        name = add.name
                        location = add.location
                        owner = if (add.personal == JumpData.TYPE_PERSONAL) user else null
                        ownerGroup = group
                        metaCreation = System.currentTimeMillis()
                        metaUpdate = System.currentTimeMillis()
                    }
                    ImageAction(add.location).get()
                }
                ctx.status(HttpStatus.CREATED_201).json(add)
            } else
                throw ConflictResponse()
        }, Auth.defaultRoleAccess)
        // Edit a jump point
        patch("${Runner.BASE}/v1/jumps/edit", { ctx ->
            val update = ctx.bodyAsClass(EditJumpData::class.java)
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val user = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            transaction {
                val existing = Jump.findById(update.id) ?: throw NotFoundResponse()

                // User can change personal jumps
                if(existing.owner == user || user.role.name == Auth.BasicRoles.ADMIN.name) {
                    existing.apply {
                        name = update.name
                        location = update.location
                        metaUpdate = System.currentTimeMillis()
                    }
                    ImageAction(update.location).get()
                    ctx.status(HttpStatus.NO_CONTENT_204).json(update)
                }
                else throw ForbiddenResponse()
            }
        }, Auth.defaultRoleAccess)
        // Delete a jump point
        delete("${Runner.BASE}/v1/jumps/rm/:id", { ctx ->
            val id = ctx.pathParam("id").toIntOrNull() ?: throw BadRequestResponse()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val user = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            transaction {
                val result = Jump.findById(id) ?: throw NotFoundResponse()
                // 403 if jump is global and user ISN'T admin
                if (result.owner == null && user.role.name != Auth.BasicRoles.ADMIN.name) throw ForbiddenResponse()
                // 403 if jump is personal and tokens don't match
                if (result.owner != null && result.owner!!.token != user.token) throw ForbiddenResponse()
                result.delete()
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, Auth.defaultRoleAccess)
    }
}
