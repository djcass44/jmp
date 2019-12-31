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

package dev.castive.jmp.service

import dev.castive.jmp.api.Responses
import dev.castive.jmp.data.AuthToken
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Session
import dev.castive.jmp.entity.User
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.repo.MetaRepo
import dev.castive.jmp.repo.SessionRepo
import dev.castive.jmp.repo.SessionRepoCustom
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.JwtTokenProvider
import dev.castive.log2.logi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JwtAuthenticationService @Autowired constructor(
	private val userRepo: UserRepo,
	private val sessionRepo: SessionRepo,
	private val sessionRepoCustom: SessionRepoCustom,
	private val metaRepo: MetaRepo,
	private val jwtTokenProvider: JwtTokenProvider
) {
	fun createToken(user: User): AuthToken {
		"Generating new request token for ${user.username}".logi(javaClass)
		val (session, request, refresh) = Session.fromUser(user, metaRepo.save(Meta.fromUser(user)), jwtTokenProvider)
		sessionRepo.save(session)
		return AuthToken(request, refresh, user.source)
	}

	fun refreshToken(refreshToken: String): AuthToken {
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
		return AuthToken(request, refresh, user.source)
	}

	fun revokeToken(token: String) {
		val username = jwtTokenProvider.getUsername(token) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		val user = userRepo.findFirstByUsername(username) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		val session = sessionRepoCustom.findFirstByUserAndRequestTokenAndActiveTrue(user, token) ?: throw NotFoundResponse(Responses.NOT_FOUND_SESSION)
		// disable the session
		sessionRepo.save(session.apply {
			active = false
		})
		"Disabled session ${session.id} owned by $username".logi(javaClass)
	}
}
