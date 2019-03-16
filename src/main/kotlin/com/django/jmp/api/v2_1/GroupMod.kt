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

package com.django.jmp.api.v2_1

import com.django.jmp.api.Auth
import com.django.jmp.api.Runner
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.auth.response.AuthenticateResponse
import com.django.jmp.db.dao.Group
import com.django.jmp.db.dao.User
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.ForbiddenResponse
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.EndpointGroup
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.collections.ArrayList

class GroupMod: EndpointGroup {
    override fun addEndpoints() {
        patch("${Runner.BASE}/v2_1/groupmod/add", { ctx ->
            val addUser = UUID.fromString(ctx.queryParam("uid"))
            val addGroup = UUID.fromString(ctx.queryParam("gid"))
            if(addUser == null || addGroup == null) throw BadRequestResponse("Invalid path parameters")
            Log.d(javaClass, "add - queryParams valid")
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse("Invalid token")
            Log.d(javaClass, "add - JWT parse valid")
            val user = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            Log.d(javaClass, "add - JWT validation passed")
            transaction {
                val newUser = User.findById(addUser) ?: throw NotFoundResponse("Invalid uid")
                val group = Group.findById(addGroup) ?: throw NotFoundResponse("Invalid gid")
                if(user.role.name == Auth.BasicRoles.ADMIN.name || group.users.contains(user)) {
                    // Add user to GroupUsers
                    val newUsers = ArrayList<User>()
                    newUsers.addAll(group.users)
                    newUsers.add(newUser)
                    group.users = SizedCollection(newUsers)
                    Log.i(javaClass, "${user.username} added ${newUser.username} to group ${group.name}")
                }
            }
        }, Auth.defaultRoleAccess)
        delete("${Runner.BASE}/v2_1/groupmod/rm", { ctx ->
            val rmUser = UUID.fromString(ctx.queryParam("uid"))
            val rmGroup = UUID.fromString(ctx.queryParam("gid"))
            if(rmUser == null || rmGroup == null) throw BadRequestResponse("Invalid path parameters")
            Log.d(javaClass, "rm - queryParams valid")
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse("Invalid token")
            Log.d(javaClass, "rm - JWT parse valid")
            val user = TokenProvider.getInstance().verify(jwt) ?: run {
                ctx.header(AuthenticateResponse.header, AuthenticateResponse.response)
                throw ForbiddenResponse("Token verification failed")
            }
            Log.d(javaClass, "rm - JWT validation passed")
            transaction {
                val oldUser = User.findById(rmUser) ?: throw NotFoundResponse("Invalid uid")
                val group = Group.findById(rmGroup) ?: throw NotFoundResponse("Invalid gid")
                if(user.role.name == Auth.BasicRoles.ADMIN.name || group.users.contains(user)) {
                    val newUsers = ArrayList<User>()
                    newUsers.addAll(group.users)
                    newUsers.remove(oldUser)
                    group.users = SizedCollection(newUsers)
                    Log.i(javaClass, "${user.username} removed ${oldUser.username} from group ${group.name}")
                }
            }
        }, Auth.defaultRoleAccess)
    }
}