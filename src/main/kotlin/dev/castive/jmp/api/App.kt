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
import dev.castive.javalin_auth.api.OAuth2
import dev.castive.javalin_auth.auth.Providers
import dev.castive.javalin_auth.auth.Roles.BasicRoles
import dev.castive.javalin_auth.auth.TokenProvider
import dev.castive.javalin_auth.auth.data.model.atlassian_crowd.CrowdCookieConfig
import dev.castive.javalin_auth.auth.provider.CrowdProvider
import dev.castive.javalin_auth.auth.provider.LDAPProvider
import dev.castive.jmp.Arguments
import dev.castive.jmp.Runner
import dev.castive.jmp.Version
import dev.castive.jmp.api.v1.Jump
import dev.castive.jmp.api.v2.Info
import dev.castive.jmp.api.v2.Oauth
import dev.castive.jmp.api.v2.Similar
import dev.castive.jmp.api.v2.User
import dev.castive.jmp.api.v2_1.Group
import dev.castive.jmp.api.v2_1.GroupMod
import dev.castive.jmp.api.v2_1.Health
import dev.castive.jmp.api.v2_1.Props
import dev.castive.jmp.audit.Logger
import dev.castive.jmp.auth.*
import dev.castive.jmp.cache.BaseCacheLayer
import dev.castive.jmp.cache.JvmCache
import dev.castive.jmp.crypto.KeyProvider
import dev.castive.jmp.crypto.SSMKeyProvider
import dev.castive.jmp.except.ExceptionTracker
import dev.castive.jmp.tasks.SocketHeartbeatTask
import dev.castive.jmp.util.EnvUtil
import dev.castive.jmp.util.asEnv
import dev.castive.log2.Log
import dev.castive.log2.loga
import dev.castive.log2.logi
import io.javalin.Javalin
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.http.HandlerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.util.*


class App(private val port: Int = 7000) {
	companion object {
		val id = UUID.randomUUID().toString()
		var exceptionTracker = ExceptionTracker(blockLeak = EnvUtil.getEnv(EnvUtil.JMP_ALLOW_ERROR_INFO, "false").toBoolean())
//		@Deprecated(message = "Config is stored by the provider")
		var crowdCookieConfig: CrowdCookieConfig? = null
		val auth = Auth()
	}

	suspend fun start(arguments: Arguments) = withContext(Dispatchers.Default) {
		val cache = JvmCache()
		val logger = Logger()
		EventLog.stream.add(System.out)
		// the key must be available for the accessManager
		setupKey()
		// Start the cache concurrently
		launch { startCache(cache) }
		// Setup providers
		val builder = ConfigBuilder().get()
		val verify = UserVerification(auth)
		val provider = when(builder.realm) {
			"ldap" -> LDAPProvider(builder.asLDAP2(), verify)
			"crowd" -> CrowdProvider(builder.asCrowd()).apply {
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
				server {
					// get our customised server
					getServer(port)
				}
				showJavalinBanner = false
				if (arguments.enableCors) { enableCorsForAllOrigins() }
				if (arguments.enableDev) { registerPlugin(RouteOverviewPlugin(Runner.BASE, setOf(BasicRoles.ANYONE))) }
				requestLogger { ctx, timeMs ->
					logger.add("${System.currentTimeMillis()} - ${ctx.method()} ${ctx.path()} took $timeMs ms")
				}
				// User our custom access manager
				accessManager(AccessManager(cache))
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
				val userUtils = UserUtils(cache)
				val ws = Socket(cache).apply {
					addEndpoints()
				}
				SocketHeartbeatTask.broadcaster = ws::fire
				// General
				Info(arguments).addEndpoints()
				Props(builder.min, exceptionTracker).addEndpoints()

				// Jumping
				Jump(ws::fire).addEndpoints()

				// Users
				User(auth, ws::fire, builder.min).addEndpoints()

				// Group
				Group(ws::fire).addEndpoints()
				GroupMod().addEndpoints()

				// Authentication
				Oauth(auth, verify, userUtils).addEndpoints()
				OAuth2(Runner.BASE, OAuth2Callback(userUtils)).addEndpoints()
//				UserMod(auth).addEndpoints()
			}
			start(port)
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

	private fun startCache(cache: BaseCacheLayer) {
		val existingID = cache.get("appId")
		if(existingID == null) cache.set("appId", UUID.randomUUID().toString())
	}

	private fun getServer(port: Int): Server {
		val server = Server()
		val secure = EnvUtil.JMP_HTTP_SECURE.asEnv().toBoolean()
		val h2 = EnvUtil.JMP_HTTP2.asEnv("true").toBoolean()
		// build an ssl or http connector based on env
		val connector = if(secure) {
			"Setting SSL context on base server".logi(javaClass)
			// prefer http2
			if(h2) {
				"Enabling HTTP2 for base server".loga(javaClass)
				getHTTP2ServerConnector(server)
			}
			else ServerConnector(server, getSslContextFactory())
		} else ServerConnector(server)
		connector.port = port
		server.connectors = arrayOf(connector)
		return server
	}

	private fun getHTTP2ServerConnector(server: Server): ServerConnector {
		val alpn = ALPNServerConnectionFactory().apply {
			defaultProtocol = "h2"
		}
		val ssl = SslConnectionFactory(getSslContextFactory(true), alpn.protocol)
		val httpsConfig = HttpConfiguration().apply {
			sendServerVersion = false
			secureScheme = "https"
			securePort = this@App.port
			addCustomizer(SecureRequestCustomizer())
		}
		val http2 = HTTP2ServerConnectionFactory(httpsConfig)
		val fallback = HttpConnectionFactory(httpsConfig)

		return ServerConnector(server, ssl, alpn, http2, fallback).apply {
			this.port = this@App.port
		}
	}

	private fun getSslContextFactory(h2: Boolean = true): SslContextFactory = SslContextFactory.Server().apply {
		keyStorePath = EnvUtil.JMP_SSL_KEYSTORE.asEnv()
		if(h2) {
			cipherComparator = HTTP2Cipher.COMPARATOR
			provider = "Conscrypt"
		}
		setKeyStorePassword(EnvUtil.JMP_SSL_PASSWORD.asEnv())
	}
}