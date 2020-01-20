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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.scribejava.apis.GoogleApi20
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import dev.castive.jmp.data.OAuth2User
import dev.castive.jmp.data.UserProjection
import dev.castive.jmp.prop.OAuth2ProviderConfig
import dev.castive.log2.logd
import dev.castive.log2.loge
import dev.dcas.util.extend.parse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service

@Service
@ConditionalOnExpression("!'\${security.oauth2.google}'.isEmpty()")
class GoogleProvider @Autowired constructor(
	// why cant I just wire an OAuth2ProviderConfig bean??
	@Value("\${security.oauth2.google.apiUrl}")
	private val apiUrl: String,
	@Value("\${security.oauth2.google.callbackUrl}")
	callbackUrl: String,
	@Value("\${security.oauth2.google.scope}")
	scope: String,
	@Value("\${security.oauth2.google.clientId}")
	private val clientId: String,
	@Value("\${security.oauth2.google.clientSecret}")
	private val clientSecret: String,
	private val objectMapper: ObjectMapper
): AbstractOAuth2Provider(OAuth2ProviderConfig(apiUrl, callbackUrl, scope, clientId, clientSecret), GoogleApi20.instance(), "google") {

	data class GoogleUser(
		val sub: String,
		val name: String,
		val picture: String,
		val iss: String,
		val aud: String,
		val exp: Long
	): OAuth2User {
		/**
		 * Creates a common interface for oauth2 user information
		 */
		override fun project(): UserProjection {
			return UserProjection(sub, name, picture, "google")
		}
	}

	override fun isTokenValid(accessToken: String): Boolean {
		val request = OAuthRequest(Verb.GET, "$apiUrl/v1/tokeninfo")
		service.signRequest(accessToken, request)
		val response = service.execute(request)
		// if the request failed, return false
		if(!response.isSuccessful) {
			"Got unsuccessful response from Google's /v1/tokeninfo".logd(javaClass)
			return false
		}
		// verify that our clientId matches the one in the token
		return objectMapper.readTree(response.body).path("audience").asText() == clientId
	}

	override fun getUserInformation(accessToken: String): UserProjection? {
		// create and sign the request
		val request = OAuthRequest(Verb.GET, "$apiUrl/v3/userinfo")
		service.signRequest(accessToken, request)
		val response = service.execute(request)
		// if the request failed, return null
		if(!response.isSuccessful) {
			"Failed to load user information: ${response.body}".loge(javaClass)
			return null
		}
		// extract the relevant information
		return response.body.parse(GoogleUser::class.java).project()
	}
}
