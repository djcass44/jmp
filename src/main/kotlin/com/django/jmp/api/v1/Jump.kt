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
import com.django.jmp.db.EditJumpData
import com.django.jmp.db.Jump
import com.django.jmp.db.JumpData
import com.django.jmp.db.Jumps
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.ConflictResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Jump(private val auth: Auth): EndpointGroup {
    private fun jumpExists(name: String): Boolean {
        return transaction {
            val existing = Jump.find {
                Jumps.name.lowerCase() eq name.toLowerCase() and Jumps.token.isNull()
            }
            return@transaction !existing.empty()
        }
    }
    private fun jumpExists(name: String, token: UUID?): Boolean {
        if(token == null)
            return jumpExists(name) // Fall back to v1 without a token
        return transaction {
            val existing = Jump.find {
                Jumps.name.lowerCase() eq name.toLowerCase() and Jumps.token.eq(token)
            }
            return@transaction if(existing.empty()) jumpExists(name) else !existing.empty() // Fall back to v1 if nothing found in v2
        }
    }

    override fun addEndpoints() {
        // List all items in Json format
        ApiBuilder.get("/v1/jumps", { ctx ->
            val items = arrayListOf<JumpData>()
            val token: String? = ctx.header(Auth.headerToken)
            val tokenUUID = if (token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            transaction {
                Jump.all().forEach {
                    if (it.token == null || it.token!! == tokenUUID)
                        items.add(JumpData(it))
                }
            }
            ctx.json(items).status(HttpStatus.OK_200)
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Redirect to $location (if it exists)
        ApiBuilder.get("/v1/jump/:target", { ctx ->
            try {
                val target = ctx.pathParam("target")
                if (target.isBlank())
                    throw EmptyPathException()
                Log.d(Runner::class.java, "Target: $target")
                /**
                 * 1. Try to get token from X-Auth-Token header
                 * 2. Try to get token from ?token=...
                 * 3. Assume no token, redirect to checker
                 */
                val token = ctx.header(Auth.headerToken) ?: ctx.queryParam("token", "") ?: ""
                if (token.isBlank()) {
                    ctx.redirect("/tokcheck.html?query=$target")
                    ctx.status(HttpStatus.FOUND_302)
                    return@get
                }
                var foundV2 = false
                if (token.isNotBlank() && token != "global" && token != "null") { // Request has a token, search user-jumps first
                    val tokenUUID = UUID.fromString(token)
                    transaction {
                        val dbtarget = Jump.find {
                            Jumps.name.lowerCase() eq target.toLowerCase() and Jumps.token.isNotNull() and Jumps.token.eq(
                                tokenUUID
                            )
                        }
                        if (!dbtarget.empty()) {
                            val location = dbtarget.elementAt(0).location
                            Log.v(javaClass, "v2: moving to user point: $location")
                            foundV2 = true
                            ctx.redirect(location, HttpStatus.FOUND_302)
                        }
                    }
                }
                if (foundV2) // Jump was found in personal collection, don't need to check global
                    return@get
                transaction {
                    val dbtarget = Jump.find {
                        Jumps.name.lowerCase() eq target.toLowerCase() and Jumps.token.isNull()
                    }
                    if (!dbtarget.empty()) {
                        val location = dbtarget.elementAt(0).location
                        Log.v(javaClass, "Redirecting to $location")
                        ctx.redirect(location, HttpStatus.FOUND_302)
                    } else
                        ctx.redirect("/similar.html?query=$target")
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e(Runner::class.java, "Invalid target: ${ctx.path()}")
                throw BadRequestResponse()
            } catch (e: EmptyPathException) {
                Log.e(Runner::class.java, "Empty target")
                throw NotFoundResponse()
            }
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Add a jump point
        ApiBuilder.put("/v1/jumps/add", { ctx ->
            val add = ctx.bodyAsClass(JumpData::class.java)
            val token: String? = ctx.header(Auth.headerToken)
            val tokenUUID = if (token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            // Block valid users with invalid tokens
            if (tokenUUID != null && !auth.validateUserToken(tokenUUID)) throw UnauthorizedResponse()
            // Block non-admin user from adding global jumps
            val user = ctx.header(Auth.headerUser)
            if (!add.personal && user != null && auth.getUserRole(user) != Auth.BasicRoles.ADMIN) throw UnauthorizedResponse()
            if (!jumpExists(add.name, tokenUUID)) {
                transaction {
                    Jump.new {
                        name = add.name
                        location = add.location
                        if (tokenUUID != null && add.personal)
                            this.token = tokenUUID
                    }
                    ImageAction(add.location).get()
                }
                ctx.status(HttpStatus.CREATED_201)
            } else
                throw ConflictResponse()
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Edit a jump point
        ApiBuilder.patch("/v1/jumps/edit", { ctx ->
            val update = ctx.bodyAsClass(EditJumpData::class.java)
            val user = ctx.header(Auth.headerUser)
            transaction {
                if (update.lastName != update.name && jumpExists(update.name)) {
                    throw ConflictResponse()
                } else {
                    val existing = Jump.find {
                        Jumps.name eq update.lastName
                    }
                    if (!existing.empty()) {
                        val item = existing.elementAt(0)
                        // Block non-admin user from editing global jumps
                        if (item.token == null && user != null && auth.getUserRole(user) != Auth.BasicRoles.ADMIN) throw UnauthorizedResponse()
                        item.name = update.name
                        item.location = update.location
                        ImageAction(update.location).get()
                        ctx.status(HttpStatus.NO_CONTENT_204).json(update)
                    } else
                        throw NotFoundResponse()
                }
            }
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Delete a jump point
        ApiBuilder.delete("/v1/jumps/rm/:name", { ctx ->
            val name = ctx.pathParam("name")
            val user = ctx.header(Auth.headerUser)
            val token: String? = ctx.header(Auth.headerToken)
            val tokenUUID = if (token != null && token.isNotBlank() && token != "null") UUID.fromString(token) else null
            val role = auth.getUserRole(user)
            transaction {
                val result = Jump.find {
                    Jumps.name eq name
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                // 401 if jump is global and user ISN'T admin
                if (result.token == null && role != Auth.BasicRoles.ADMIN) throw UnauthorizedResponse()
                // 401 if jump is personal and tokens don't match
                if (result.token != null && result.token != tokenUUID) throw UnauthorizedResponse()
                Jumps.deleteWhere {
                    Jumps.id eq result.id
                }
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
    }
}