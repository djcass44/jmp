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
import io.javalin.core.compression.Brotli
import io.javalin.core.compression.Gzip
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.core.security.Role
import org.eclipse.jetty.http.HttpStatus
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


class App(private val port: Int = 7000) {
	companion object {
		val id = UUID.randomUUID().toString()
		var exceptionTracker = ExceptionTracker(blockLeak = true)
//		@Deprecated(message = "Config is stored by the provider")
		var crowdCookieConfig: CrowdCookieConfig? = null
		val auth = Auth()
	}
	fun start(store: ConfigStore, arguments: Arguments, logger: Logger) {
		EventLog.stream.add(System.out)
		transaction {
			SchemaUtils.create(Jumps, Users, Roles, Groups, GroupUsers, Aliases) // Ensure that the tables are created
			Log.i(javaClass, "Checking for database drift")
			SchemaUtils.createMissingTablesAndColumns(Jumps, Users, Roles, Groups, GroupUsers, Aliases, Sessions)
			Init(store) // Ensure that the default admin/roles is created
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
		// Start the cache concurrently
		startCache()
		Javalin.create { config ->
			config.apply {
				showJavalinBanner = false
				if (arguments.enableCors) { enableCorsForAllOrigins() }
				if (arguments.enableDev) { registerPlugin(RouteOverviewPlugin(Runner.BASE, setOf(Auth.BasicRoles.ANYONE))) }
				// experimental brotli compression
				compressionStrategy(Brotli(4), Gzip(6))
				requestLogger { ctx, timeMs ->
					logger.add("${System.currentTimeMillis()} - ${ctx.method()} ${ctx.path()} took $timeMs ms")
				}
				// User our custom access manager
				accessManager(AccessManager())
			}
		}.apply {
			exception(Exception::class.java) { e, ctx ->
				Log.e(javaClass, "Encountered unhandled exception: $e")
				if(Log.getPriorityLevel() <= 1) // only print for debug/verbose
					e.printStackTrace()
				exceptionTracker.onExceptionTriggered(e)
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
			}
			routes {
				val ws = WebSocket()
				ws.addEndpoints()
				// General
				Info().addEndpoints()
				Props(builder, exceptionTracker).addEndpoints()

				// Jumping
				Jump(ws).addEndpoints()
				Similar().addEndpoints()

				// Users
				User(auth, ws, builder.min).addEndpoints()

				// Group
				Group(ws).addEndpoints()
				GroupMod().addEndpoints()

				// Authentication
				Oauth(auth, verify).addEndpoints()
//				UserMod(auth).addEndpoints()

				// Health
				Health(builder.min).addEndpoints()
			}
			start()
		}
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

	private fun startCache() {
		AuthAction.cacheLayer.setup()
		val existingID = AuthAction.cacheLayer.getMisc("appId")
		if(existingID == null) AuthAction.cacheLayer.setMisc("appId", UUID.randomUUID().toString())

		// Reseed the JWT signer with our encryption key
		val keyProvider = getKeyProvider()
		Log.v(javaClass, "Using key provider: ${keyProvider::class.java.name}")
		TokenProvider.buildSigner(keyProvider.getEncryptionKey())

		// Gracefully shutdown the cache layer when the JVM is shutting down
		Runtime.getRuntime().addShutdownHook(Thread {
			Log.w(javaClass, "Shutting down cache layer")
			AuthAction.cacheLayer.tearDown()
		})
	}
}