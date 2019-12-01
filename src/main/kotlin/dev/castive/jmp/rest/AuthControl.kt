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
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Session
import dev.castive.jmp.except.BadRequestResponse
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.except.UnauthorizedResponse
import dev.castive.jmp.repo.MetaRepo
import dev.castive.jmp.repo.SessionRepo
import dev.castive.jmp.repo.SessionRepoCustom
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.JwtTokenProvider
import dev.castive.log2.logi
import dev.dcas.util.extend.isESNullOrBlank
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/a2")
class AuthControl @Autowired constructor(
	private val userRepo: UserRepo,
	private val sessionRepo: SessionRepo,
	private val sessionRepoCustom: SessionRepoCustom,
	private val metaRepo: MetaRepo,
	private val jwtTokenProvider: JwtTokenProvider
) {
	@PostMapping("/login", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun createToken(@RequestBody basic: BasicAuth): AuthToken {
		val user = userRepo.findFirstByUsername(basic.username) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		// only allow JWT generation for local users
		if(user.hash.isESNullOrBlank() || user.source != "local")
			throw BadRequestResponse("Cannot generate token for this user")
		if(!Hash.password(basic.password.toCharArray()).verify(user.hash))
			throw UnauthorizedResponse("Incorrect username or password")
		"Generating new request token for ${user.username}".logi(javaClass)
		val (session, request, refresh) = Session.fromUser(user, metaRepo.save(Meta.fromUser(user)), jwtTokenProvider)
		sessionRepo.save(session)
		return AuthToken(request, refresh, "local")
	}

	@GetMapping("/refresh", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun refreshToken(@RequestParam(required = true) refreshToken: String): AuthToken {
		val username = jwtTokenProvider.getUsername(refreshToken) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		val user = userRepo.findFirstByUsername(username) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		// must have an existing session in order to refresh
		val existingSession = sessionRepoCustom.findFirstByUserAndRefreshTokenAndActiveTrue(user, refreshToken) ?: throw NotFoundResponse(Responses.NOT_FOUND_SESSION)
		// invalidate the old session
		sessionRepo.save(existingSession.apply {
			active = false
		})
		// create a new session
		val (session, request, refresh) = Session.fromUser(user, metaRepo.save(Meta.fromUser(user)), jwtTokenProvider)
		sessionRepo.save(session)
		"Generating new refresh token for ${user.username}".logi(javaClass)
		return AuthToken(request, refresh, "local")
	}

	@PreAuthorize("hasRole('USER')")
	@PostMapping("/logout", produces = [MediaType.TEXT_PLAIN_VALUE])
	fun revokeToken(request: HttpServletRequest): String {
		val token = jwtTokenProvider.resolveToken(request) ?: throw UnauthorizedResponse()
		val username = jwtTokenProvider.getUsername(token) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		val user = userRepo.findFirstByUsername(username) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		val session = sessionRepoCustom.findFirstByUserAndRequestTokenAndActiveTrue(user, token) ?: throw NotFoundResponse(Responses.NOT_FOUND_SESSION)
		// disable the session
		sessionRepo.save(session.apply {
			active = false
		})
		"Disabled session ${session.id} owned by $username".logi(javaClass)
		return "OK"
	}
}
