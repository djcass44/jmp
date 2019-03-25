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

import com.django.log2.logging.Log
import dev.castive.jmp.api.Runner
import dev.castive.jmp.auth.JWTContextMapper
import dev.castive.jmp.auth.Providers
import dev.castive.jmp.auth.TokenProvider
import dev.castive.jmp.auth.response.AuthenticateResponse
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.db.dao.User
import io.javalin.BadRequestResponse
import io.javalin.Context
import io.javalin.ForbiddenResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class User(private val auth: dev.castive.jmp.api.Auth, private val providers: Providers): EndpointGroup {
    private fun assertUser(ctx: Context): User {
        val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: run {
            ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
            throw ForbiddenResponse("Token verification failed")
        }
        Log.d(javaClass, "JWT parse valid")
        return TokenProvider.getInstance().verify(jwt) ?: run {
            ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
            throw ForbiddenResponse("Token verification failed")
        }
    }

    override fun addEndpoints() {
        get("${Runner.BASE}/v2/users", { ctx ->
            assertUser(ctx)
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
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
        // Add a user
        put("${Runner.BASE}/v2/user/add", { ctx ->
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: ""
            val user = if(jwt == "null" || jwt.isBlank()) null else TokenProvider.getInstance().verify(jwt)
            transaction {
                if ((user == null || auth.getUserRole(user.username, user.token) == dev.castive.jmp.api.Auth.BasicRoles.USER) && !providers.keyedProps[Providers.PROP_EXT_ALLOW_LOCAL]!!.toBoolean()) {
                    Log.i(javaClass, "User ${user?.username} is not allowed to create local accounts [reason: POLICY]")
                    throw UnauthorizedResponse("Creating local accounts has been disabled.")
                }
            }
            val basicAuth = ctx.basicAuthCredentials() ?: throw BadRequestResponse()
            auth.createUser(basicAuth.username, basicAuth.password.toCharArray())
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
        // Get information about the current user
        get("${Runner.BASE}/v2/user", { ctx ->
            val u = assertUser(ctx)
            transaction {
                ctx.status(HttpStatus.OK_200).result(u.role.name)
            }
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2_1/user/info", { ctx ->
            val u = assertUser(ctx)
            transaction {
                ctx.status(HttpStatus.OK_200).json(UserData(u, arrayListOf()))
            }
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
        // Get a users token
        post("${Runner.BASE}/v2/user/auth", { ctx ->
            ctx.status(HttpStatus.MOVED_PERMANENTLY_301).result("This has been deprecated in favour of OAuth2 /v2/oauth")
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
        // Change the role of a user
        patch("${Runner.BASE}/v2/user/permission", { ctx ->
            val updated = ctx.bodyAsClass(EditUserData::class.java)
            val u = assertUser(ctx)
            transaction {
                val user = User.findById(updated.id) ?: throw BadRequestResponse()
                // Block dropping the superuser from admin
                if(user.username == "admin") throw ForbiddenResponse()
                // Block the user from changing their own permissions
                if(user.username == u.username) throw ForbiddenResponse()
                val role = Role.find {
                    Roles.name eq updated.role
                }.elementAtOrNull(0) ?: throw BadRequestResponse()
                Log.i(javaClass, "User role updated [user: ${user.username}, from: ${user.role.name}, to: ${role.name}] by ${u.username}")
                user.role = role
                user.metaUpdate = System.currentTimeMillis()
                ctx.status(HttpStatus.NO_CONTENT_204).json(updated)
            }
        }, roles(dev.castive.jmp.api.Auth.BasicRoles.ADMIN))
        // Delete a user
        delete("${Runner.BASE}/v2/user/rm/:id", { ctx ->
            val id = UUID.fromString(ctx.pathParam("id"))
            val user = assertUser(ctx)
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
        }, roles(dev.castive.jmp.api.Auth.BasicRoles.ADMIN))
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
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
    }
}