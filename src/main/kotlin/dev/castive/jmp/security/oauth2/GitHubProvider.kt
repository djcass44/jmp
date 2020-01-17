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

import com.github.scribejava.apis.GitHubApi
import dev.castive.jmp.prop.OAuth2ProviderConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service

@Service
@ConditionalOnExpression("!'\${security.oauth2.github}'.isEmpty()")
class GitHubProvider(
	// why cant I just wire an OAuth2ProviderConfig bean??
	@Value("\${security.oauth2.github.callbackUrl}")
	callbackUrl: String,
	@Value("\${security.oauth2.github.scope}")
	scope: String,
	@Value("\${security.oauth2.github.clientId")
	clientId: String,
	@Value("\${security.oauth2.github.clientSecret")
	clientSecret: String
): AbstractOAuth2Provider(OAuth2ProviderConfig(callbackUrl, scope, clientId, clientSecret), GitHubApi.instance(), "github") {

	override fun isTokenValid(accessToken: String): Boolean {
		return false
	}
}
