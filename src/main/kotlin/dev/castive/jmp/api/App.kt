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

import dev.castive.eventlog.EventLog
import dev.castive.javalin_auth.actions.UserAction
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.CrowdCookieConfig
import dev.castive.javalin_auth.auth.provider.CrowdProvider
import dev.castive.javalin_auth.auth.provider.LDAPProvider
import dev.castive.jmp.Arguments
import dev.castive.jmp.Runner
import dev.castive.jmp.Version
import dev.castive.jmp.api.actions.AuthAction
import dev.castive.jmp.api.v1.Jump
import dev.castive.jmp.api.v2.Info
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.api.v2.Similar
import dev.castive.jmp.api.v2.User
import dev.castive.jmp.api.v2_1.*
import dev.castive.jmp.api.v2_1.Group
import dev.castive.jmp.audit.Logger
import dev.castive.jmp.auth.AccessManager
import dev.castive.jmp.auth.LDAPConfigBuilder
import dev.castive.jmp.auth.UserValidator
import dev.castive.jmp.auth.UserVerification
import dev.castive.jmp.crypto.KeyProvider
import dev.castive.jmp.crypto.SSMKeyProvider
import dev.castive.jmp.db.ConfigStore
import dev.castive.jmp.db.Init
import dev.castive.jmp.db.dao.*
import dev.castive.jmp.except.ExceptionTracker
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import io.javalin.Javalin
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.HandlerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


class App(private val port: Int = 7000) {
	companion object {
		val id = UUID.randomUUID().toString()
		var exceptionTracker = ExceptionTracker(blockLeak = EnvUtil.getEnv(EnvUtil.JMP_ALLOW_ERROR_INFO, "false").toBoolean())
//		@Deprecated(message = "Config is stored by the provider")
		var crowdCookieConfig: CrowdCookieConfig? = null
		val auth = Auth()
	}
	private lateinit var ws: WebSocket

	/**
	 * Request sending a message to the websocket server
	 * This is used to ensure that messages aren't send to an un-initialised server
	 */
	private fun wsRequest(tag: String, data: Any) {
		if(!this::ws.isInitialized) {
			Log.w(javaClass, "Socket server is not ready, request $tag will be dropped")
			return
		}
		ws.fire(tag, data)
	}

	suspend fun start(store: ConfigStore, arguments: Arguments, logger: Logger) = withContext(Dispatchers.Default) {
		EventLog.stream.add(System.out)
		// the key must be available for the accessManager
		setupKey()
		// Start the cache concurrently
		launch { startCache() }
		// Use the IO pool because the database will likely be doing IO operations
		withContext(Dispatchers.IO) {
			transaction {
				SchemaUtils.create(
					Jumps,
					Users,
					Roles,
					Groups,
					GroupUsers,
					Aliases
				) // Ensure that the tables are created
				Log.i(javaClass, "Checking for database drift")
				SchemaUtils.createMissingTablesAndColumns(Jumps, Users, Roles, Groups, GroupUsers, Aliases, Sessions)
				Init(store) // Ensure that the default admin/roles is created
			}
		}
		val builder = LDAPConfigBuilder(store)
		val verify = UserVerification(auth)
		val provider = if(!builder.min.enabled) null else when(builder.type) {
			"ldap" -> LDAPProvider(builder.ldapConfig, verify)
			"crowd" -> CrowdProvider(builder.crowdConfig).apply {
				this.setup()
				crowdCookieConfig = this.getSSOConfig() as CrowdCookieConfig?
			}
			else -> null
		}
		Providers(builder.min, provider).init(verify) // Setup user authentication
		Providers.validator = UserValidator(auth, builder.min)
//	    TokenProvider.ageProfile = TokenProvider.TokenAgeProfile.DEV
		UserAction.verification = verify
		val javalinStart = System.currentTimeMillis()
		Javalin.create { config ->
			config.apply {
				showJavalinBanner = false
				if (arguments.enableCors) { enableCorsForAllOrigins() }
				if (arguments.enableDev) { registerPlugin(RouteOverviewPlugin(Runner.BASE, setOf(Auth.BasicRoles.ANYONE))) }
				requestLogger { ctx, timeMs ->
					logger.add("${System.currentTimeMillis()} - ${ctx.method()} ${ctx.path()} took $timeMs ms")
				}
				// User our custom access manager
				accessManager(AccessManager())
			}
		}.apply {
			exception(Exception::class.java) { e, ctx ->
				Log.e(javaClass, "Encountered unhandled exception: $e")
				Log.v(javaClass, e.printStackTrace().toString())
				exceptionTracker.onExceptionTriggered(e)
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
			}
			addHandler(HandlerType.GET, "${Runner.BASE}/v2/similar/:query", Similar(), Auth.openAccessRole)
			addHandler(HandlerType.GET, "${Runner.BASE}/v3/health", Health(builder.min), Auth.openAccessRole)
			routes {
				// General
				Info(store, arguments).addEndpoints()
				Props(builder, exceptionTracker).addEndpoints()

				// Jumping
				Jump(this@App::wsRequest).addEndpoints()

				// Users
				User(auth, this@App::wsRequest, builder.min).addEndpoints()

				// Group
				Group(this@App::wsRequest).addEndpoints()
				GroupMod().addEndpoints()

				// Authentication
				Oauth(auth, verify).addEndpoints()
				Oauth2().addEndpoints()
//				UserMod(auth).addEndpoints()
			}
			start(port)
		}
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
		return when(EnvUtil.getEnv(EnvUtil.KEY_REALM, KeyProvider.shortName)) {
			SSMKeyProvider.shortName -> SSMKeyProvider()
			else -> KeyProvider()
		}
	}

	private fun setupKey() {
		// Reseed the JWT signer with our encryption key
		val keyProvider = getKeyProvider()
		Log.v(javaClass, "Using key provider: ${keyProvider::class.java.name}")
		TokenProvider.buildSigner(keyProvider.getEncryptionKey())
	}

	private fun startCache() {
		AuthAction.cacheLayer.setup()
		val existingID = AuthAction.cacheLayer.getMisc("appId")
		if(existingID == null) AuthAction.cacheLayer.setMisc("appId", UUID.randomUUID().toString())

		// Gracefully shutdown the cache layer when the JVM is shutting down
		Runtime.getRuntime().addShutdownHook(Thread {
			Log.w(javaClass, "Shutting down cache layer")
			AuthAction.cacheLayer.tearDown()
		})
		// Start the websocket server
		ws = WebSocket().apply {
			start()
		}
	}
}