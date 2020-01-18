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

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.scribejava.apis.GitHubApi
import com.google.gson.annotations.SerializedName
import dev.castive.jmp.data.OAuth2User
import dev.castive.jmp.data.UserProjection
import dev.castive.jmp.prop.OAuth2ProviderConfig
import dev.dcas.util.extend.toBasic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Service
@ConditionalOnExpression("!'\${security.oauth2.github}'.isEmpty()")
class GitHubProvider @Autowired constructor(
	// why cant I just wire an OAuth2ProviderConfig bean??
	@Value("\${security.oauth2.github.apiUrl}")
	private val apiUrl: String,
	@Value("\${security.oauth2.github.callbackUrl}")
	callbackUrl: String,
	@Value("\${security.oauth2.github.scope}")
	scope: String,
	@Value("\${security.oauth2.github.clientId}")
	private val clientId: String,
	@Value("\${security.oauth2.github.clientSecret}")
	private val clientSecret: String,
	private val restTemplate: RestTemplate
): AbstractOAuth2Provider(OAuth2ProviderConfig(apiUrl, callbackUrl, scope, clientId, clientSecret), GitHubApi.instance(), "github") {

	data class GitHubUser(
		val login: String,
		val id: Int,
		@JsonProperty("avatar_url")
		@SerializedName("avatar_url")
		val avatarUrl: String,
		val type: String,
		val name: String
	): OAuth2User {
		/**
		 * Creates a common interface for oauth2 user information
		 */
		override fun project(): UserProjection {
			return UserProjection(login, name, avatarUrl, "github")
		}
	}

	override fun isTokenValid(accessToken: String): Boolean {
		val headers = LinkedMultiValueMap<String, String>().apply {
			add("Authorization", (clientId to clientSecret).toBasic())
		}
		val response = restTemplate.exchange("${apiUrl}/applications/$clientId/tokens/$accessToken", HttpMethod.GET, HttpEntity<Any>(headers), Any::class.java)
		return response.statusCode == HttpStatus.OK
	}

	override fun getUserInformation(accessToken: String): UserProjection? {
		val headers = LinkedMultiValueMap<String, String>().apply {
			add("Authorization", "Bearer $accessToken")
		}
		val response = restTemplate.exchange("${apiUrl}/user", HttpMethod.GET, HttpEntity<Any>(headers), GitHubUser::class.java)
		return if(validateUserResponse(response))
			response.body!!.project()
		else
			null
	}
}
