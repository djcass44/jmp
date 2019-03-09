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
import com.django.jmp.api.Similar
import com.django.jmp.auth.JWTContextMapper
import com.django.jmp.auth.TokenProvider
import com.django.jmp.db.dao.Jump
import com.django.jmp.db.dao.Jumps
import com.django.jmp.except.EmptyPathException
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

class Similar : EndpointGroup {
    override fun addEndpoints() {
        // Find similar jumps
        get("${Runner.BASE}/v2/similar/:query", { ctx ->
            try {
                val jwt = ctx.use(JWTContextMapper::class.java).tokenAuthCredentials(ctx) ?: ""
                val user = if(jwt.isBlank()) null else TokenProvider.getInstance().verify(jwt)
                val query = ctx.pathParam("query")
                if (query.isBlank())
                    throw EmptyPathException()
                val names = arrayListOf<String>()
                transaction {
                    val res = Jump.find {
                        Jumps.owner.isNull() or Jumps.owner.eq(user?.id)
                    }
                    res.forEach { names.add(it.name) }
                }
                val similar = Similar(query, names)
                ctx.status(HttpStatus.OK_200).json(similar.compute())
            }
            catch (e: EmptyPathException) {
                Log.e(Runner::class.java, "Empty target")
                throw BadRequestResponse()
            }
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
    }
}