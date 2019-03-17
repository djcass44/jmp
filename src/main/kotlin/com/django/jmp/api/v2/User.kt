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
import com.django.jmp.auth.response.AuthenticateResponse
import com.django.jmp.db.dao.*
import com.django.jmp.db.dao.User
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.ForbiddenResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class User(private val auth: Auth): EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2/users", { ctx ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            Log.d(javaClass, "list - JWT parse valid")
            TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            Log.d(javaClass, "list - JWT validation passed")
            val users = arrayListOf<UserData>()
            transaction {
                User.all().forEach {
                    val res = (Groups innerJoin GroupUsers innerJoin Users)
                        .slice(Groups.columns)
                        .select {
                            Users.id eq it.id
                        }
                        .withDistinct()
                    val groups = arrayListOf<String>()
                    Group.wrapRows(res).toList().forEach { g ->
                        groups.add(g.name)
                    }
                    users.add(UserData(it, groups))
                }
            }
            ctx.status(HttpStatus.OK_200).json(users)
        }, Auth.defaultRoleAccess)
        // Add a user
        put("${Runner.BASE}/v2/user/add", { ctx ->
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            auth.createUser(basicAuth.username, basicAuth.password.toCharArray())
        }, Auth.defaultRoleAccess)
        // Get information about the current user
        get("${Runner.BASE}/v2/user", { ctx ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val u = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            transaction {
                ctx.status(HttpStatus.OK_200).result(u.role.name)
            }
        }, Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2_1/user/info", { ctx ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val u = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            transaction {
                ctx.status(HttpStatus.OK_200).json(UserData(u, arrayListOf()))
            }
        }, Auth.defaultRoleAccess)
        // Get a users token
        post("${Runner.BASE}/v2/user/auth", { ctx ->
            ctx.status(HttpStatus.MOVED_PERMANENTLY_301).result("This has been deprecated in favour of OAuth2 /v2/oauth")
        }, Auth.defaultRoleAccess)
        // Change the role of a user
        patch("${Runner.BASE}/v2/user/permission", { ctx ->
            val updated = ctx.bodyAsClass(EditUserData::class.java)
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val u = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            transaction {
                val user = User.findById(updated.id) ?: throw BadRequestResponse()
                // Block dropping the superuser from admin
                if(user.username == "admin") throw ForbiddenResponse()
                // Block the user from changing their own permissions
                if(user.username == u.username) throw ForbiddenResponse()
                val role = Role.find {
                    Roles.name eq updated.role
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                user.role = role
                user.metaUpdate = System.currentTimeMillis()
                ctx.status(HttpStatus.NO_CONTENT_204).json(updated)
            }
        }, roles(Auth.BasicRoles.ADMIN))
        // Delete a user
        delete("${Runner.BASE}/v2/user/rm/:id", { ctx ->
            val id = UUID.fromString(ctx.pathParam("id"))
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            val user = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
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
            ctx.status(HttpStatus.NO_CONTENT_204)
        }, roles(Auth.BasicRoles.ADMIN))
        get("${Runner.BASE}/v2_1/user/groups", { ctx ->
            val uid = runCatching {
                UUID.fromString(ctx.queryParam("uid"))
            }.getOrElse {
                throw BadRequestResponse("Bad UUID")
            }
            val items = arrayListOf<GroupData>()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: ""
            val user = if(jwt == "null" || jwt.isBlank()) null else TokenProvider.getInstance().verify(jwt)
            if(user != null) {
                transaction {
                    val getUser = User.findById(uid) ?: throw BadRequestResponse("Requested user is null")
                    val res = (Groups innerJoin GroupUsers innerJoin Users)
                        .slice(Groups.columns)
                        .select {
                            Users.id eq getUser.id
                        }
                        .withDistinct()
                    Group.wrapRows(res).toList().forEach {
                        items.add(GroupData(it))
                    }
                }
            }
            ctx.status(HttpStatus.OK_200).json(items)
        }, Auth.defaultRoleAccess)
    }
}