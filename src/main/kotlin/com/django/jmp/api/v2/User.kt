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
            ctx.status(HttpStatus.OK_200).json(users)
        }, roles(Auth.BasicRoles.ADMIN))
        // Add a user
        put("/v2/user/add", { ctx ->
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            auth.createUser(basicAuth.username, basicAuth.password.toCharArray())
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
                ctx.status(HttpStatus.OK_200).result("NONE")
                return@get // User doesn't exist, stop here
            }
            if (auth.validateUserToken(tokenUUID)) {
                val role = auth.getUserRole(user)
                ctx.status(HttpStatus.OK_200).result(role.toString())
            } else throw UnauthorizedResponse()
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Get a users token
        post("/v2/user/auth", { ctx ->
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            val token = auth.getUserToken(basicAuth.username, basicAuth.password.toCharArray())
            if (token != null)
                ctx.json(token)
            else
                throw NotFoundResponse()
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Change the role of a user
        patch("/v2/user/permission", { ctx ->
            val updated = ctx.bodyAsClass(EditUserData::class.java)
            transaction {
                val user = User.findById(updated.id) ?: throw BadRequestResponse()
                // Block dropping the superuser from admin
                if(user.username == "admin") throw UnauthorizedResponse()
                // Block the user from changing their own permissions
                if(user.username == ctx.header(Auth.headerUser)) throw UnauthorizedResponse()
                val role = Role.find {
                    Roles.name eq updated.role
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                user.role = role
                ctx.status(HttpStatus.NO_CONTENT_204).json(updated)
            }
        }, roles(Auth.BasicRoles.ADMIN))
        // Delete a user
        delete("/v2/user/rm/:id", { ctx ->
            val id = ctx.validatedPathParam("id").asInt().getOrThrow()
            val user = ctx.header(Auth.headerUser)
            transaction {
                val target = User.findById(id) ?: throw BadRequestResponse()
                Log.i(javaClass, "[$user] is removing ${target.username}")
                if(target.username == "admin") {
                    Log.w(javaClass, "[$user] is attempting to remove the superuser")
                    throw UnauthorizedResponse()
                } // Block deleting the superuser
                if(target.username == user) {
                    Log.w(javaClass, "[$user] is attempting to remove themselves")
                    throw UnauthorizedResponse()
                } // Stop the users deleting themselves
                target.delete()
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, roles(Auth.BasicRoles.ADMIN))
    }
}