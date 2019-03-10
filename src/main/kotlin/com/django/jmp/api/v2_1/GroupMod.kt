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
import com.django.jmp.db.dao.Group
import com.django.jmp.db.dao.User
import io.javalin.BadRequestResponse
import io.javalin.NotFoundResponse
import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil.roles
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction

class GroupMod: EndpointGroup {
    override fun addEndpoints() {
        patch("${Runner.BASE}/v2_1/groupmod/add", { ctx ->
            val addUser = ctx.validatedPathParam("uid").asInt().value
            val addGroup = ctx.validatedPathParam("gid").asInt().value
            if(addUser == null || addGroup == null) throw BadRequestResponse("Invalid path parameters")
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse("Invalid token")
            val user = TokenProvider.getInstance().verify(jwt) ?: throw BadRequestResponse("Token verification failed")
            transaction {
                val newUser = User.findById(addUser) ?: throw NotFoundResponse("Invalid uid")
                val group = Group.findById(addGroup) ?: throw NotFoundResponse("Invalid gid")
                if(user.role.name == Auth.BasicRoles.ADMIN.name || group.users.contains(user)) {
                    // Add user to GroupUsers
                    val newUsers = ArrayList<User>()
                    newUsers.addAll(group.users)
                    newUsers.add(newUser)
                    group.users = SizedCollection(newUsers)
                }
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        delete("${Runner.BASE}/v2_1/groupmod/rm", { ctx ->
            val rmUser = ctx.validatedPathParam("uid").asInt().value
            val rmGroup = ctx.validatedPathParam("gid").asInt().value
            if(rmUser == null || rmGroup == null) throw BadRequestResponse("Invalid path parameters")
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse("Invalid token")
            val user = TokenProvider.getInstance().verify(jwt) ?: throw BadRequestResponse("Token verification failed")
            transaction {
                val oldUser = User.findById(rmUser) ?: throw NotFoundResponse("Invalid uid")
                val group = Group.findById(rmGroup) ?: throw NotFoundResponse("Invalid gid")
                if(user.role.name == Auth.BasicRoles.ADMIN.name || group.users.contains(user)) {
                    val newUsers = ArrayList<User>()
                    newUsers.addAll(group.users)
                    newUsers.remove(oldUser)
                    group.users = SizedCollection(newUsers)
                }
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
    }
}