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
import dev.castive.jmp.db.dao.Jumps
import dev.castive.jmp.db.repo.findAllByUser
import dev.castive.jmp.util.ok
import dev.castive.jmp.util.user
import dev.castive.log2.Log
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.Handler

class Similar: Handler {
    private val similar = Similar()

    override fun handle(ctx: Context) {
        val query = ctx.pathParam("query", String::class.java).getOrNull()
        // we require a search term, so error if we dont get it
        if (query.isNullOrBlank()) {
            Log.i(javaClass, "Received null or empty target request from: ${ctx.userAgent()}")
            throw BadRequestResponse("Empty or null target")
        }
        // get the jumps visible to the user
        val userJumps = Jumps.findAllByUser(ctx.user(), includeAliases = true)

        // check if the user wants suggestions instead (only the name)
        val suggest = ctx.queryParam("suggest", Boolean::class.java).getOrNull()
        ctx.ok().json(
            if(suggest != true)
                similar.compute(userJumps, query)
            else
                similar.computeNames(userJumps, query)
        )
    }
}