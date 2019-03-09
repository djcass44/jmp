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
import com.django.jmp.db.dao.User
import com.django.jmp.db.dao.Users
import com.django.log2.logging.Log
import io.javalin.BadRequestResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.security.SecurityUtil
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class Verify(private val auth: Auth): EndpointGroup {
    override fun addEndpoints() {
        // Verify a users token is still valid
        get("${Runner.BASE}/v2/verify/token", { ctx ->
            ctx.status(HttpStatus.MOVED_PERMANENTLY_301).result("This has been deprecated in favour of OAuth2 /v2/oauth")
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
        // Verify a user still exists
        get("${Runner.BASE}/v2/verify/user/:name", { ctx ->
            val name = ctx.pathParam("name")
            if (name.isBlank()) {
                Log.v(Runner::class.java, "User made null/empty request")
                throw BadRequestResponse()
            }
            transaction {
                val result = User.find {
                    Users.username eq name
                }.elementAtOrNull(0)
                if (result == null) {
                    Log.w(javaClass, "User: $name failed verification [IP: ${ctx.ip()}, UA: ${ctx.userAgent()}]")
                    throw BadRequestResponse()
                } else {
                    if (auth.validateUserToken(result.token)) {
                        ctx.status(HttpStatus.OK_200).result(name)
                    } else {
                        Log.w(
                            javaClass,
                            "User: $name exists, however their token is invalid [IP: ${ctx.ip()}, UA: ${ctx.userAgent()}]"
                        )
                        throw BadRequestResponse()
                    }
                }
            }
        }, SecurityUtil.roles(Auth.BasicRoles.USER, Auth.BasicRoles.ADMIN))
    }
}