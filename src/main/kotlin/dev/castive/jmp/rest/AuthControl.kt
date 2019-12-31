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

import com.amdelamar.jhash.Hash
import dev.castive.jmp.api.Responses
import dev.castive.jmp.data.AuthToken
import dev.castive.jmp.data.BasicAuth
import dev.castive.jmp.except.BadRequestResponse
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.except.UnauthorizedResponse
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.JwtTokenProvider
import dev.castive.jmp.security.SecurityConstants
import dev.castive.jmp.service.JwtAuthenticationService
import dev.castive.jmp.service.auth.LdapService
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.dcas.util.extend.isESNullOrBlank
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/a2")
class AuthControl @Autowired constructor(
	private val userRepo: UserRepo,
	private val ldapService: LdapService,
	private val jwtTokenProvider: JwtTokenProvider,
	private val authService: JwtAuthenticationService
) {
	@PostMapping("/login", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
	fun createToken(@RequestBody basic: BasicAuth): AuthToken {
		// try ldap first
		ldapService.getUserByName(basic)?.let {
			"Located user in LDAP: ${it.username}".logi(javaClass)
			return authService.createToken(it)
		}
		"Attempting to locate user in database: ${basic.username}".logv(javaClass)
		// fallback to a standard database user
		val user = userRepo.findFirstByUsername(basic.username) ?: run {
			"Failed to locate user in database: ${basic.username}".logi(javaClass)
			throw NotFoundResponse(Responses.NOT_FOUND_USER)
		}
		// only allow JWT generation for local users
		if(user.hash.isESNullOrBlank() || user.source != SecurityConstants.sourceLocal)
			throw BadRequestResponse("Cannot generate token for this user")
		if(!Hash.password(basic.password.toCharArray()).verify(user.hash))
			throw UnauthorizedResponse("Incorrect username or password")
		return authService.createToken(user)
	}

	@GetMapping("/refresh", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun refreshToken(@RequestParam(required = true) refreshToken: String): AuthToken {
		return authService.refreshToken(refreshToken)
	}

	@PreAuthorize("hasRole('USER')")
	@PostMapping("/logout", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun revokeToken(request: HttpServletRequest): ResponseEntity<Nothing> {
		val token = jwtTokenProvider.resolveToken(request) ?: throw UnauthorizedResponse()
		authService.revokeToken(token)
		return ResponseEntity.noContent().build()
	}
}
