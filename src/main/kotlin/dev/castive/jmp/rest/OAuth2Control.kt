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
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.security.oauth2.AbstractOAuth2Provider
import dev.castive.log2.logi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct

@RequestMapping("/o2")
@RestController
class OAuth2Control @Autowired constructor(private val providers: List<AbstractOAuth2Provider>) {

	@PostConstruct
	fun init() {
		"Found ${providers.size} oauth2 provider(s): [${providers.joinToString(", ") { it.name }}]".logi(javaClass)
	}

	/**
	 * Check whether a provider exists
	 * Used by the front end for showing social login buttons
	 */
	@GetMapping("/api/{name}")
	fun getProvider(@PathVariable("name") name: String): String {
		return findProviderByName(name)?.name ?: throw NotFoundResponse(Responses.NOT_FOUND_PROVIDER)
	}

	/**
	 * Redirect the user to an oauth2 provider consent screen
	 * No handling is done here, that is done by the callback
	 * Note: the user will hit this endpoint directly
	 */
	@GetMapping("/{name}", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getProviderUrl(@PathVariable("name") name: String): Pair<String, String> {
		val provider = findProviderByName(name) ?: throw NotFoundResponse(Responses.NOT_FOUND_PROVIDER)
		return provider.name to provider.getAuthoriseUrl()
	}

	private fun findProviderByName(name: String): AbstractOAuth2Provider? = providers.firstOrNull {
		it.name == name
	}
}
