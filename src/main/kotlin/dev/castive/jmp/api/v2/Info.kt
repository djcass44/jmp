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

import dev.castive.jmp.Arguments
import dev.castive.jmp.Runner
import dev.castive.jmp.Version
import dev.castive.jmp.api.Auth
import dev.castive.jmp.api.actions.InfoAction
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.util.ok
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.BadRequestResponse
import io.javalin.http.NotFoundResponse
import org.eclipse.jetty.http.HttpStatus

class Info(private val store: ConfigStore, private val arguments: Arguments): EndpointGroup {
    override fun addEndpoints() {
        // Version/info
        get("${Runner.BASE}/v2/version", { ctx ->
            ctx.status(HttpStatus.OK_200).result("v${Version.getVersion()}")
        }, Auth.openAccessRole)
        // get application/system information
        get("${Runner.BASE}/v2/info/:type", { ctx ->
            val type = ctx.pathParam("type", String::class.java).getOrNull() ?: throw BadRequestResponse("Invalid or null type")
            val info = InfoAction(store, arguments)
            ctx.ok().json(when(type) {
                "system" -> info.getSystem()
                "app" -> info.getApp()
                else -> throw NotFoundResponse()
            })
        }, Auth.adminRoleAccess)
    }
}