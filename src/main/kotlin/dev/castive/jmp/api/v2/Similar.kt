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

import dev.castive.javalin_auth.actions.UserAction
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Similar
import dev.castive.jmp.api.actions.OwnerAction
import dev.castive.jmp.auth.ClaimConverter
import dev.castive.jmp.except.EmptyPathException
import dev.castive.log2.Log
import io.javalin.BadRequestResponse
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus

class Similar : EndpointGroup {
    override fun addEndpoints() {
        // Find similar jumps
        get("${Runner.BASE}/v2/similar/:query", { ctx ->
            try {
                val user = ClaimConverter.getUser(UserAction.getOrNull(ctx))
                val query = ctx.pathParam("query")
                if (query.isBlank())
                    throw EmptyPathException()
                val userJumps = OwnerAction.getUserVisibleJumps(user, includeAliases = true)
                val similar = Similar(query, userJumps)
                similar.compute()
                ctx.status(HttpStatus.OK_200).json(similar.results)
            }
            catch (e: EmptyPathException) {
                Log.e(javaClass, "Empty target")
                throw BadRequestResponse()
            }
        }, dev.castive.jmp.api.Auth.defaultRoleAccess)
    }
}