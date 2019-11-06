/*
 *    Copyright [2019 Django Cass
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

package dev.castive.jmp.api.config

import dev.castive.jmp.util.EnvUtil
import dev.castive.jmp.util.asEnv
import dev.castive.log2.loga
import dev.castive.log2.logi
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory

class ServerConfig(private val port: Int) {
	/**
	 * Get the customised server
	 * Configures SSL, HTTP/2
	 */
	fun getServer(): Server {
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
			securePort = this@ServerConfig.port
			addCustomizer(SecureRequestCustomizer())
		}
		val http2 = HTTP2ServerConnectionFactory(httpsConfig)
		val fallback = HttpConnectionFactory(httpsConfig)

		return ServerConnector(server, ssl, alpn, http2, fallback).apply {
			this.port = this@ServerConfig.port
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