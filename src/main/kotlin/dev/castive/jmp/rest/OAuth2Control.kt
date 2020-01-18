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

package dev.castive.jmp.rest

import dev.castive.jmp.api.Responses
import dev.castive.jmp.data.AuthToken
import dev.castive.jmp.except.BadRequestResponse
import dev.castive.jmp.except.InternalErrorResponse
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.security.SecurityConstants
import dev.castive.jmp.security.oauth2.AbstractOAuth2Provider
import dev.castive.jmp.service.auth.OAuth2Service
import dev.castive.jmp.util.ellipsize
import dev.castive.log2.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@RequestMapping("/o2")
@RestController
class OAuth2Control @Autowired constructor(
	private val providers: List<AbstractOAuth2Provider>,
	private val oauth2Service: OAuth2Service
) {

	@PostConstruct
	fun init() {
		"Found ${providers.size} oauth2 provider(s): [${providers.joinToString(", ") { it.name }}]".logi(javaClass)
	}

	/**
	 * Check whether a provider exists
	 * Used by the front end for showing social login buttons
	 */
	@GetMapping("/api/{name}")
	fun getProvider(@PathVariable("name") name: String): String = findProviderByNameOrThrow(name).name

	@GetMapping("/callback")
	fun createToken(@RequestParam("code", required = true) code: String, @RequestParam("state", required = true) state: String): AuthToken {
		val (name, _, _) = AbstractOAuth2Provider.parseState(state)
		"Got callback request for provider: $name".logv(javaClass)
		// get the provider
		val provider = findProviderByNameOrThrow(name)
		// create our token
		val token = kotlin.runCatching {
			provider.getAccessToken(code)
		}.onFailure {
			"Failed to extract access token from callback data".loge(javaClass, it)
			throw BadRequestResponse("Unable to extract access token")
		}.getOrThrow()
		// attempt to create the user
		if(oauth2Service.createUser(token.accessToken, token.refreshToken ?: token.accessToken, provider))
			return AuthToken(token.accessToken, token.refreshToken ?: token.accessToken, "oauth2/${provider.name}")
		else {
			"Failed to create user from token: ${token.accessToken.ellipsize(24)}".loge(javaClass)
			throw InternalErrorResponse()
		}
	}

	/**
	 * Redirect the user to an oauth2 provider consent screen
	 * No handling is done here, that is done by the callback
	 * Note: the user will hit this endpoint directly
	 */
	@GetMapping("/{name}", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getProviderUrl(@PathVariable("name") name: String): Pair<String, String> {
		val provider = findProviderByNameOrThrow(name)
		return provider.name to provider.getAuthoriseUrl()
	}

	@PreAuthorize("hasRole('USER')")
	@PostMapping("/logout/{name}")
	fun revokeToken(@PathVariable("name") name: String, @RequestParam("accessToken", required = true) accessToken: String) {
		val provider = findProviderByNameOrThrow(name)
		"Performing logout for OAuth2 token: ${accessToken.ellipsize(24)}".loga(javaClass)
		/* sometimes revoking the token is unsupported and throws an exception
		   this is okay and we still want to return the 200
		 */
		kotlin.runCatching {
			provider.revokeTokenAsync(accessToken)
		}.onFailure {
			"Failed to perform logout using provider: $name, it is probably unsupported".logw(javaClass)
		}
	}

	/**
	 * Find an OAuth2 provider by its common name (e.g. 'github', 'google')
	 * @param name: the name of the provider
	 * @return AbstractOAuth2Provider or throw http 404 if it can't be found
	 */
	private fun findProviderByNameOrThrow(name: String): AbstractOAuth2Provider = findProviderByName(name) ?: throw NotFoundResponse(Responses.NOT_FOUND_PROVIDER)

	/**
	 * Find an OAuth2 provider by its common name (e.g. 'github', 'google')
	 * @param name: the name of the provider
	 * @return AbstractOAuth2Provider or null if it can't be found
	 */
	private fun findProviderByName(name: String): AbstractOAuth2Provider? = providers.firstOrNull {
		it.name == name
	}
}
