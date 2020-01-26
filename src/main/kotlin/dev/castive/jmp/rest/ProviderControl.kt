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

package dev.castive.jmp.rest

import dev.castive.jmp.repo.UserRepo
import dev.dcas.jmp.spring.security.SecurityConstants
import dev.dcas.jmp.spring.security.oauth2.impl.AbstractOAuth2Provider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CacheConfig(cacheNames = ["providers"])
@RestController
@RequestMapping("/v2/providers")
class ProviderControl @Autowired constructor(
	private val userRepo: UserRepo,
	private val providers: List<AbstractOAuth2Provider>
) {

	private val internalProviders = arrayListOf(SecurityConstants.sourceLocal, SecurityConstants.sourceLdap).apply {
		addAll(providers.map { "oauth2/${it.name}" })
	}

	@Cacheable
	@PreAuthorize("hasRole('USER')")
	@GetMapping
	fun getAll(): List<Pair<String, Int>> = internalProviders.map {
		it to userRepo.countAllBySource(it)
	}
}
