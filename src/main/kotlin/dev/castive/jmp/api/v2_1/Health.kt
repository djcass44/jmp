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

import com.google.common.util.concurrent.RateLimiter
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.connect.MinimalConfig
import dev.castive.javalin_auth.auth.provider.InternalProvider
import dev.castive.jmp.api.App
import dev.castive.jmp.api.Responses
import dev.castive.jmp.auth.AccessManager
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.util.checks.DatabaseCheck
import dev.castive.jmp.util.checks.FavCheck
import dev.castive.jmp.util.checks.ProviderCheck
import dev.castive.jmp.util.ok
import dev.castive.log2.Log
import io.javalin.http.Context
import io.javalin.http.Handler
import org.eclipse.jetty.http.HttpStatus

class Health(private val config: MinimalConfig): Handler {
	// Used for JSON response to the front-end
	data class HealthPayload(
		val code: Int = HttpStatus.OK_200,
		val http: String = "OK",
		val database: Boolean?,
		val identityProvider: Boolean?,
		val providerName: String,
		val imageApi: Boolean? = null
	)

	private val rateLimiter = RateLimiter.create(10.0)

	override fun handle(ctx :Context) {
		// allow health checks from any origin
		// this is fine because we are rate-limiting this endpoint
		ctx.header("Access-Control-Allow-Origin", "*")
		val user: User? = ctx.attribute(AccessManager.attributeUser)
		when {
			App.auth.isAdmin(user) -> {
				Log.v(javaClass, "Getting health check for an admin")
				val check = runChecks(safe = false) // Allow the admin to run database checks
				Log.d(javaClass, check.toString())
				ctx.ok().json(check)
			}
			rateLimiter.tryAcquire() -> {
				Log.v(javaClass, "Getting health check for a normal user, they haven't been rate limited")
				val check = runChecks()
				Log.d(javaClass, check.toString())
				ctx.ok().json(check)
			}
			else -> ctx.status(HttpStatus.TOO_MANY_REQUESTS_429).result(Responses.GENERIC_RATE_LIMITED)
		}
	}
	private fun runChecks(safe: Boolean = true): HealthPayload {
		val dbCheck = if(!safe) DatabaseCheck().runCheck() else null
		val providerCheck = if(config.enabled) ProviderCheck().runCheck() else null
		val favStatus = if(!safe) FavCheck().runCheck() else null
		val providerName = if(config.enabled) Providers.primaryProvider?.getName() ?: InternalProvider.SOURCE_NAME else InternalProvider.SOURCE_NAME
		val code = if(dbCheck == false || providerCheck == false) HttpStatus.INTERNAL_SERVER_ERROR_500 else HttpStatus.OK_200

		return HealthPayload(code, "OK", dbCheck, providerCheck, providerName, favStatus)
	}
}