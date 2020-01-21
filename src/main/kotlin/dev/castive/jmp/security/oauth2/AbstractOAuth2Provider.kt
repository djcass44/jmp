/*
 *    Copyright 2020 Django Cass
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

package dev.castive.jmp.security.oauth2

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.model.OAuth2AccessToken
import dev.castive.jmp.data.UserProjection
import dev.castive.jmp.prop.OAuth2ProviderConfig
import dev.castive.log2.logv
import dev.dcas.util.extend.base64Url
import dev.dcas.util.extend.decodeBase64Url
import dev.dcas.util.extend.randomString
import java.util.concurrent.Future
import javax.annotation.PostConstruct

abstract class AbstractOAuth2Provider(
	private val provider: OAuth2ProviderConfig,
	api: DefaultApi20
) {
	companion object {
		fun parseState(state: String): Triple<String, String, String> {
			val (name ,meta, code) = state.decodeBase64Url().split(":", limit = 3)
			return Triple(name, meta, code)
		}
	}

	val name: String = provider.name

	internal val service = ServiceBuilder(provider.clientId)
		.apiSecret(provider.clientSecret)
		.callback(provider.callbackUrl)
		.defaultScope(provider.scope)
		.build(api)

	@PostConstruct
	fun init() {
		"Initialising bean for OAuth2 provider: ${provider.name}".logv(javaClass)
	}

	/**
	 * Get the url for the consent screen to redirect the user
	 */
	fun getAuthoriseUrl(): String = service.getAuthorizationUrl(getState())

	/**
	 * Get an access token using the consent code
	 */
	fun getAccessToken(code: String): OAuth2AccessToken = service.getAccessToken(code)

	/**
	 * Get a new access token using our refresh token
	 */
	fun refreshToken(refreshToken: String): OAuth2AccessToken = service.refreshAccessToken(refreshToken)

	/**
	 * Used for logout
	 * Revokes the oauth2 token (assuming the api supports revoking tokens)
	 * this::revokeTokenAsync is preferred
	 */
	open fun revokeToken(accessToken: String) = service.revokeToken(accessToken)

	/**
	 * Used for logout
	 * Async is preferred because the user isn't waiting on the result
	 */
	open fun revokeTokenAsync(accessToken: String): Future<*> = service.revokeTokenAsync(accessToken)

	/**
	 * Check if the access token is still valid
	 */
	abstract fun isTokenValid(accessToken: String): Boolean

	abstract fun getUserInformation(accessToken: String): UserProjection?

	private fun getState(meta: String = ""): String = "${provider.name}:$meta:${32.randomString()}".base64Url()
}
