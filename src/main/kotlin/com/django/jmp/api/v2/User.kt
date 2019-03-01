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

package com.django.jmp.api.v2

import com.django.jmp.api.Auth
import com.django.jmp.api.Runner
import com.django.jmp.db.*
import com.django.jmp.db.User
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class User(private val auth: Auth): EndpointGroup {
    override fun addEndpoints() {
        get("/v2/users", { ctx ->
            val users = arrayListOf<UserData>()
            transaction {
                User.all().forEach {
                    users.add(UserData(it))
                }
            }
            ctx.json(users).status(HttpStatus.OK_200)
        }, roles(Auth.BasicRoles.ADMIN))
        // Add a user
        put("/v2/user/add", { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            auth.createUser(credentials.username, credentials.password)
        }, roles(Auth.BasicRoles.ADMIN))
        // Get information about the current user
        get("/v2/user", { ctx ->
            val user = ctx.header(Auth.headerUser)
            val token = ctx.header(Auth.headerToken)
            if (user == null || token == null)
                throw BadRequestResponse()
            val tokenUUID = try {
                UUID.fromString(token)
            } catch (e: Exception) {
                Log.e(Runner::class.java, "User: $user provided malformed token [IP: ${ctx.ip()}, UA: ${ctx.userAgent()}]")
                throw BadRequestResponse()
            }
            if (!auth.userExists(user)) {
                ctx.result("NONE").status(HttpStatus.OK_200)
                return@get // User doesn't exist, stop here
            }
            if (auth.validateUserToken(tokenUUID)) {
                val role = auth.getUserRole(user)
                ctx.result(role.toString()).status(HttpStatus.OK_200)
            } else throw UnauthorizedResponse()
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Get a users token
        post("/v2/user/auth", { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            val token = auth.getUserToken(credentials.username, credentials.password)
            if (token != null)
                ctx.json(token)
            else
                throw NotFoundResponse()
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Change the role of a user
        patch("/v2/user/permission", { ctx ->
            val updated = ctx.bodyAsClass(EditUserData::class.java)
            // Block dropping the superuser from admin
            if(updated.username == "admin") throw BadRequestResponse()
            // Block the user from changing their own permissions
            if(updated.username == ctx.header(Auth.headerUser)) throw UnauthorizedResponse()
            transaction {
                val role = Role.find {
                    Roles.name eq updated.role
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                val user = User.find {
                    Users.username eq updated.username
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                user.role = role
                ctx.status(HttpStatus.NO_CONTENT_204).json(updated)
            }
        }, roles(Auth.BasicRoles.ADMIN))
        // Delete a user
        delete("/v2/user/rm/:name", { ctx ->
            val name = ctx.pathParam("name")
            if(name == "admin") throw UnauthorizedResponse() // Block deleting the superuser
            val user = ctx.header(Auth.headerUser)
            if(name == user) throw UnauthorizedResponse() // Stop the users deleting themselves
            transaction {
                Users.deleteWhere {
                    Users.username eq name
                }
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, roles(Auth.BasicRoles.ADMIN))
    }
}