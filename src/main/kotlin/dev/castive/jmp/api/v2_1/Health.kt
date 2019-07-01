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
import dev.castive.jmp.Runner
import dev.castive.jmp.api.Auth
import dev.castive.jmp.util.checks.DatabaseCheck
import dev.castive.jmp.util.checks.ProviderCheck
import dev.castive.log2.Log
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import org.eclipse.jetty.http.HttpStatus

class Health(private val config: MinimalConfig): EndpointGroup {
	private val rateLimiter = RateLimiter.create(5.0)

	override fun addEndpoints() {
		get("${Runner.BASE}/v3/health", { ctx ->
			if(rateLimiter.tryAcquire()) {
				val check = runChecks()
				Log.d(javaClass, check.toString())
				ctx.status(HttpStatus.OK_200).json(check)
			}
			else
				ctx.status(HttpStatus.TOO_MANY_REQUESTS_429).result("You're making too many requests")
		}, Auth.openAccessRole)
	}
	private fun runChecks(): HealthPayload {
		val dbCheck = DatabaseCheck().runCheck()
		val providerCheck = if(config.enabled) ProviderCheck().runCheck() else null
		val providerName = if(config.enabled) Providers.primaryProvider?.getName() ?: InternalProvider.SOURCE_NAME else InternalProvider.SOURCE_NAME
		val code = if(!dbCheck || providerCheck == false) HttpStatus.INTERNAL_SERVER_ERROR_500 else HttpStatus.OK_200

		return HealthPayload(code, "OK", dbCheck, providerCheck, providerName)
	}
}
data class HealthPayload(val code: Int = HttpStatus.OK_200, val http: String = "OK", val database: Boolean, val identityProvider: Boolean?, val providerName: String)