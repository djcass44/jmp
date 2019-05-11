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

package dev.castive.jmp.api.v2_1

import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.provider.LDAPProvider
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.auth.LDAPConfigBuilder
import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.Groups
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.transactions.transaction

class Props(private val builder: LDAPConfigBuilder): EndpointGroup {
    override fun addEndpoints() {
        get("${Runner.BASE}/v2_1/prop/:target", { ctx ->
            val targetProp = ctx.pathParam("target")
            val result = when {
                builder.properties.containsKey(targetProp) -> builder.properties.getProperty(targetProp)
                else -> builder.properties.getOrDefault(targetProp, "undefined")
            }
            ctx.status(HttpStatus.OK_200).result(result.toString())
        }, Auth.adminRoleAccess)
        get("${Runner.BASE}/v2_1/uprop/allow_local", { ctx ->
            ctx.status(HttpStatus.OK_200).result(builder.extra.blockLocal.not().toString())
        }, Auth.defaultRoleAccess)
        get("${Runner.BASE}/v2_1/provider/main", { ctx ->
            val connected = Providers.primaryProvider != null && Providers.primaryProvider!!.connected()
            val users = transaction { return@transaction User.find { Users.from eq LDAPProvider.SOURCE_NAME }.count() }
            val groups = transaction { return@transaction Group.find { Groups.from eq LDAPProvider.SOURCE_NAME }.count() }
            ctx.status(HttpStatus.OK_200).json(LDAPPayload(connected, users, groups))
        }, Auth.adminRoleAccess)
    }
}
data class LDAPPayload(val connected: Boolean, val users: Int, val groups: Int)