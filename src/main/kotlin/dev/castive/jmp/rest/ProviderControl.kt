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

import dev.castive.log2.logd
import dev.dcas.jmp.security.shim.repo.UserRepo
import dev.dcas.jmp.spring.security.SecurityConstants
import dev.dcas.jmp.spring.security.props.SecurityProps
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct

@CacheConfig(cacheNames = ["providers"])
@RestController
@RequestMapping("/v2/providers")
class ProviderControl(
	private val userRepo: UserRepo,
	private val oauth2Config: SecurityProps
) {

	private val internalProviders = arrayListOf(SecurityConstants.sourceLocal, SecurityConstants.sourceLdap)

	@PostConstruct
	fun init() {
		"Located ${oauth2Config.oauth2.size} providers".logd(javaClass)
		internalProviders.apply {
			addAll(oauth2Config.oauth2.map { "oauth2/${it.name}" })
		}
	}

	@Cacheable
	@PreAuthorize("hasRole('USER')")
	@GetMapping
	fun getAll(): List<Pair<String, Int>> = internalProviders.map {
		it to userRepo.countAllBySource(it)
	}
}
