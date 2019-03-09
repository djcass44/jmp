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
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.db.dao.*
import com.django.jmp.db.dao.User
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class User(private val auth: Auth): EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2/users", { ctx ->
            val users = arrayListOf<UserData>()
            transaction {
                User.all().forEach {
                    users.add(UserData(it))
                }
            }
            ctx.status(HttpStatus.OK_200).json(users)
        }, roles(Auth.BasicRoles.ADMIN))
        // Add a user
        put("${Runner.BASE}/v2/user/add", { ctx ->
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            auth.createUser(basicAuth.username, basicAuth.password.toCharArray())
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Get information about the current user
        get("${Runner.BASE}/v2/user", { ctx ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse()
            val u = TokenProvider.getInstance().verify(jwt) ?: throw UnauthorizedResponse()
            transaction {
                ctx.status(HttpStatus.OK_200).result(u.role.name)
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Get a users token
        post("${Runner.BASE}/v2/user/auth", { ctx ->
            ctx.status(HttpStatus.MOVED_PERMANENTLY_301).result("This has been deprecated in favour of OAuth2 /v2/oauth")
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Change the role of a user
        patch("${Runner.BASE}/v2/user/permission", { ctx ->
            val updated = ctx.bodyAsClass(EditUserData::class.java)
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse()
            val u = TokenProvider.getInstance().verify(jwt) ?: throw UnauthorizedResponse()
            transaction {
                val user = User.findById(updated.id) ?: throw BadRequestResponse()
                // Block dropping the superuser from admin
                if(user.username == "admin") throw UnauthorizedResponse()
                // Block the user from changing their own permissions
                if(user.username == u.username) throw UnauthorizedResponse()
                val role = Role.find {
                    Roles.name eq updated.role
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                user.role = role
                ctx.status(HttpStatus.NO_CONTENT_204).json(updated)
            }
        }, roles(Auth.BasicRoles.ADMIN))
        // Delete a user
        delete("${Runner.BASE}/v2/user/rm/:id", { ctx ->
            val id = ctx.validatedPathParam("id").asInt().getOrThrow()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse()
            val user = TokenProvider.getInstance().verify(jwt) ?: throw UnauthorizedResponse()
            transaction {
                val target = User.findById(id) ?: throw BadRequestResponse()
                Log.i(javaClass, "[${user.username}] is removing ${target.username}")
                if(target.username == "admin") {
                    Log.w(javaClass, "[${user.username}] is attempting to remove the superuser")
                    throw UnauthorizedResponse()
                } // Block deleting the superuser
                if(target.username == user.username) {
                    Log.w(javaClass, "[${user.username}] is attempting to remove themselves")
                    throw UnauthorizedResponse()
                } // Stop the users deleting themselves
                target.delete()
            }
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, roles(Auth.BasicRoles.ADMIN))
    }
}