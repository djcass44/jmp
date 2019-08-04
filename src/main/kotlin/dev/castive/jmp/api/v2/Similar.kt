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

import dev.castive.jmp.api.Similar
import dev.castive.jmp.api.actions.OwnerAction
import dev.castive.jmp.auth.AccessManager
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.except.EmptyPathException
import dev.castive.log2.Log
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus

class Similar: Handler {
    override fun handle(ctx: Context) {
        try {
            val user: User? = ctx.attribute(AccessManager.attributeUser)
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
    }
}