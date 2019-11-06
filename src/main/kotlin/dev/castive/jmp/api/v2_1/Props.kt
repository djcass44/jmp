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

import dev.castive.javalin_auth.api.OAuth2
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.connect.MinimalConfig
import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.db.dao.Group
import dev.castive.jmp.db.dao.Groups
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.except.ExceptionTracker
import dev.castive.jmp.util.ok
import dev.castive.log2.logi
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit

class Props(private val minConfig: MinimalConfig, private val tracker: ExceptionTracker): EndpointGroup {
	data class ProviderPayload(val connected: Boolean, val name: String, val users: Int, val groups: Int)

	override fun addEndpoints() {
		get("${Runner.BASE}/v2_1/uprop/allow_local", { ctx ->
			ctx.ok().result(minConfig.blockLocal.toString())
		}, Auth.openAccessRole)
		get("${Runner.BASE}/v2_1/provider/main", { ctx ->
			val main = Providers.primaryProvider
			val connected = main != null && main.connected()
			val users = if(main != null) transaction { User.find { Users.from eq main.getName() }.count() } else 0
			val groups = if(main != null) transaction { Group.find { Groups.from eq main.getName() }.count() } else 0
			ctx.ok().json(ProviderPayload(connected, main?.getName() ?: InternalProvider.SOURCE_NAME, users, groups))
		}, Auth.adminRoleAccess)
		get("${Runner.BASE}/v2_1/statistics/providers", { ctx ->
			// get the names of all possible auth providers
			ctx.ok().json(arrayListOf(InternalProvider.SOURCE_NAME).apply {
				if(Providers.primaryProvider != null)
					add(Providers.primaryProvider!!.getName())
				// add oauth2 providers
				addAll(OAuth2.providers.keys)
			})
		}, Auth.defaultRoleAccess)
		get("${Runner.BASE}/v2_1/statistics/exception", { ctx ->
			val time = ctx.queryParam("time", "5")?.toLongOrNull() ?: 5
			"An admin is viewing all logged exceptions from the last $time minutes".logi(javaClass)
			val res = tracker.getData(TimeUnit.MINUTES.toMillis(time))
			ctx.ok().json(res)
		}, Auth.adminRoleAccess)
	}
}