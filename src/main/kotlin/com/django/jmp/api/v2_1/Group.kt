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
import com.django.jmp.db.dao.*
import com.django.jmp.db.dao.Group
import io.javalin.BadRequestResponse
import io.javalin.ConflictResponse
import io.javalin.NotFoundResponse
import io.javalin.UnauthorizedResponse
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil.roles
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Group: EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2_1/groups", { ctx ->
            val items = arrayListOf<GroupData>()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: ""
            val user = if(jwt == "null" || jwt.isBlank()) null else TokenProvider.getInstance().verify(jwt)
            if(user != null) {
                transaction {
                    if(user.role.name == Auth.BasicRoles.ADMIN.name) {
                        Group.all().forEach {
                            items.add(GroupData((it)))
                        }
                        return@transaction
                    }
                    val res = (Groups innerJoin GroupUsers innerJoin Users)
                        .slice(Groups.columns)
                        .select {
                            Users.id eq user.id
                        }
                        .withDistinct()
                    val groups = Group.wrapRows(res).toList()
                    groups.forEach {
                        items.add(GroupData(it))
                    }
                }
            }
            ctx.status(HttpStatus.OK_200).json(items)
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        get("${Runner.BASE}/v2_1/group/:id", { ctx ->
            // Only allow users to view groups they're already in
            ctx.status(HttpStatus.FORBIDDEN_403).result("This endpoint is unfinished, or not ready for public use.")
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        put("${Runner.BASE}/v2_1/group/add", { ctx ->
            val add = ctx.bodyAsClass(GroupData::class.java)
            transaction {
                val existing = Group.find {
                    Groups.name eq add.name
                }
                if(existing.count() > 0) throw ConflictResponse("Group already exists")
                Group.new {
                    name = add.name
                }
            }
            ctx.status(HttpStatus.CREATED_201).json(add)
        }, roles(Auth.BasicRoles.ADMIN))
        patch("${Runner.BASE}/v2_1/group/edit", { ctx ->
            val update = ctx.bodyAsClass(GroupData::class.java)
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse("JWT couldn't be parsed")
            val user = TokenProvider.getInstance().verify(jwt) ?: throw BadRequestResponse()
            transaction {
                val existing = Group.findById(update.id) ?: throw NotFoundResponse("Group not found")
                // Only allow update if user belongs to group (or is admin)
                if(user.role.name != Auth.BasicRoles.ADMIN.name && !existing.users.contains(user)) throw UnauthorizedResponse("User not in requested group")
                existing.name = update.name
                ctx.status(HttpStatus.NO_CONTENT_204).json(update)
            }
        }, roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        delete("${Runner.BASE}/v2_1/group/rm/:id", { ctx ->
            val id = ctx.validatedPathParam("id").asInt().getOrThrow()
            val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: throw BadRequestResponse("JWT couldn't be parsed")
            val user = TokenProvider.getInstance().verify(jwt) ?: throw BadRequestResponse()
            transaction {
                val existing = Group.findById(id) ?: throw NotFoundResponse("Group not found")
                // Only allow deletion if user belongs to group (or is admin)
                if(user.role.name != Auth.BasicRoles.ADMIN.name && !existing.users.contains(user)) throw UnauthorizedResponse("User not in requested group")
                existing.delete()
                ctx.status(HttpStatus.NO_CONTENT_204)
            }
        }, roles(Auth.BasicRoles.ADMIN))
    }
}