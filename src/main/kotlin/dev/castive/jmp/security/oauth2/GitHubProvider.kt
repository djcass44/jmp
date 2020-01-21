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
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.google.gson.annotations.SerializedName
import dev.castive.jmp.data.OAuth2User
import dev.castive.jmp.data.UserProjection
import dev.castive.jmp.prop.OAuth2ProviderConfig
import dev.castive.jmp.security.SecurityConstants
import dev.castive.log2.loge
import dev.dcas.util.extend.parse
import dev.dcas.util.extend.toBasic

class GitHubProvider(private val config: OAuth2ProviderConfig): AbstractOAuth2Provider(config, GitHubApi.instance()) {

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
		val request = OAuthRequest(Verb.GET, "${config.apiUrl}/applications/${config.clientId}/tokens/$accessToken").apply {
			addHeader(SecurityConstants.authHeader, (config.clientId to config.clientSecret).toBasic())
		}
		val response = service.execute(request)
		return response.isSuccessful
	}

	override fun getUserInformation(accessToken: String): UserProjection? {
		val request = OAuthRequest(Verb.GET, "${config.apiUrl}/user").apply {
			addHeader(SecurityConstants.authHeader, "Bearer $accessToken")
		}
		val response = service.execute(request)
		if(!response.isSuccessful) {
			"Failed to load user information: ${response.body}".loge(javaClass)
			return null
		}
		return kotlin.runCatching {
			response.body.parse(GitHubUser::class.java).project()
		}.onFailure {
			"Failed to parse response body".loge(javaClass, it)
		}.getOrNull()
	}
}
