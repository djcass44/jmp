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

package dev.castive.jmp.api

import dev.castive.javalin_auth.api.OAuth2
import dev.castive.javalin_auth.util.SigningKey
import dev.castive.jmp.Arguments
import dev.castive.jmp.Runner
import dev.castive.jmp.Version
import dev.castive.jmp.api.config.ServerConfig
import dev.castive.jmp.api.v1.Jump
import dev.castive.jmp.api.v2.Info
import dev.castive.jmp.api.v2.Similar
import dev.castive.jmp.api.v2.User
import dev.castive.jmp.api.v2_1.Group
import dev.castive.jmp.api.v2_1.GroupMod
import dev.castive.jmp.api.v2_1.Health
import dev.castive.jmp.api.v2_1.Props
import dev.castive.jmp.auth.*
import dev.castive.jmp.crypto.KeyProvider
import dev.castive.jmp.crypto.SSMKeyProvider
import dev.castive.jmp.except.ExceptionTracker
import dev.castive.jmp.tasks.SocketHeartbeatTask
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import dev.castive.log2.logd
import dev.castive.log2.logw
import dev.dcas.util.extend.env
import io.javalin.Javalin
import io.javalin.http.HandlerType
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.info.License
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jetty.http.HttpStatus
import java.util.*
import io.swagger.v3.oas.models.info.Info as SwaggerInfo


class App(private val appConfig: ConfigBuilder.JMPConfiguration) {
	companion object {
		val id = UUID.randomUUID().toString()
		var exceptionTracker = ExceptionTracker(blockLeak = EnvUtil.JMP_ALLOW_ERROR_INFO.env("false").toBoolean())
		val auth = Auth()
	}

	suspend fun start(arguments: Arguments) = withContext(Dispatchers.Default) {
		// the key must be available for the accessManager
		setupKey()
		val javalinStart = System.currentTimeMillis()
		Javalin.create { config ->
			config.apply {
				server {
					// get our customised server
					ServerConfig(appConfig.jmp.server).getServer()
				}
				showJavalinBanner = false
				if (arguments.enableCors) { enableCorsForAllOrigins() }
				val urls = EnvUtil.JMP_PROXY_URL.env("")
				if(urls.isNotBlank()) {
					"Enabling CORS for the following URLs: $urls".logw(javaClass)
					enableCorsForOrigin(*urls.split(",").toTypedArray())
				}
				registerPlugin(OpenApiPlugin(getOpenApiOptions()))
				requestLogger { ctx, timeMs ->
					"${System.currentTimeMillis()} - ${ctx.method()} ${ctx.path()} took $timeMs ms".logd(javaClass)
				}
				// User our custom access manager
				accessManager(AccessManager(appConfig))
			}
		}.apply {
			exception(Exception::class.java) { e, ctx ->
				Log.e(javaClass, "Encountered unhandled exception: $e")
				Log.v(javaClass, e.printStackTrace().toString())
				exceptionTracker.onExceptionTriggered(e)
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
			}
			addHandler(HandlerType.GET, "${Runner.BASE}/v2/similar/:query", Similar(), Auth.openAccessRole)
			addHandler(HandlerType.GET, "${Runner.BASE}/v3/health", Health(), Auth.openAccessRole)
			routes {
				val ws = Socket().apply {
					addEndpoints()
				}
				SocketHeartbeatTask.broadcaster = ws::fire
				// General
				Info(arguments).addEndpoints()
				Props(appConfig, exceptionTracker).addEndpoints()

				// Jumping
				Jump(ws::fire).addEndpoints()

				// Users
				User(auth, ws::fire, appConfig).addEndpoints()

				// Group
				Group(ws::fire).addEndpoints()
				GroupMod().addEndpoints()

				// Authentication
				dev.castive.javalin_auth.api.Auth(
					Runner.BASE,
					UserLocation(),
					SessionLocation(),
					appConfig.ldap,
					appConfig.crowd
				).addEndpoints()
				OAuth2(Runner.BASE, OAuth2Callback(), appConfig.oauth2).addEndpoints()
			}
			start(appConfig.jmp.server.port)
		}
		SocketHeartbeatTask.start()
		Log.i(javaClass, "HTTP server ready in ${System.currentTimeMillis() - javalinStart} ms")
		println("       _ __  __ _____  \n" +
				"      | |  \\/  |  __ \\ \n" +
				"      | | \\  / | |__) |\n" +
				"  _   | | |\\/| |  ___/ \n" +
				" | |__| | |  | | |     \n" +
				"  \\____/|_|  |_|_|     \n" +
				"                       \n" +
				"JMP v${Version.getVersion()} is ready (took ${System.currentTimeMillis() - Runner.START_TIME} ms)")
	}

	private fun getKeyProvider(): KeyProvider {
		return when(EnvUtil.KEY_REALM.env(KeyProvider.shortName)) {
			SSMKeyProvider.shortName -> SSMKeyProvider()
			else -> KeyProvider()
		}
	}

	private fun setupKey() {
		// Reseed the JWT signer with our encryption key
		val keyProvider = getKeyProvider()
		Log.v(javaClass, "Using key provider: ${keyProvider::class.java.name}")
		SigningKey.newKey(keyProvider.getEncryptionKey())
	}

	private fun getOpenApiOptions() = OpenApiOptions(SwaggerInfo().apply {
		license = License().name("Apache License 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")
		title = "JMP"
		version = "0.5"
		description = "Utility for quickly navigating to websites & addresses"
	}).path("/swagger-docs").swagger(SwaggerOptions("/${Runner.BASE}")).roles(Auth.openAccessRole)
}
