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
import com.django.jmp.db.User
import com.django.jmp.db.UserData
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class User(private val auth: Auth): EndpointGroup {
    override fun addEndpoints() {
        ApiBuilder.get("/v2/users", { ctx ->
            val users = arrayListOf<UserData>()
            transaction {
                User.all().forEach {
                    users.add(UserData(it))
                }
            }
            ctx.json(users)
            ctx.status(HttpStatus.OK_200)
        }, SecurityUtil.roles(Auth.BasicRoles.ADMIN, Auth.BasicRoles.USER))
        // Add a user
        ApiBuilder.put("/v2/user/add", { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            auth.createUser(credentials.username, credentials.password)
        }, SecurityUtil.roles(Auth.BasicRoles.ADMIN))
        // Get information about the current user
        ApiBuilder.get("/v2/user", { ctx ->
            val user = ctx.header(Auth.headerUser)
            val token = ctx.header(Auth.headerToken)
            if (user == null || token == null)
                throw BadRequestResponse()
            val tokenUUID = try {
                UUID.fromString(token)
            } catch (e: Exception) {
                Log.e(
                    Runner::class.java,
                    "User: $user provided malformed token [IP: ${ctx.ip()}, UA: ${ctx.userAgent()}]"
                )
                throw BadRequestResponse()
            }
            if (!auth.userExists(user)) {
                ctx.result("NONE")
                ctx.status(HttpStatus.OK_200)
                return@get // User doesn't exist, stop here
            }
            if (auth.validateUserToken(tokenUUID)) {
                val role = auth.getUserRole(user)
                ctx.result(role.toString())
                ctx.status(HttpStatus.OK_200)
            } else throw UnauthorizedResponse()
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Get a users token
        ApiBuilder.post("/v2/user/auth", { ctx ->
            val credentials = Auth.BasicAuth(ctx.bodyAsClass(Auth.BasicAuth.Insecure::class.java))
            val token = auth.getUserToken(credentials.username, credentials.password)
            if (token != null)
                ctx.json(token)
            else
                throw NotFoundResponse()
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
    }
}