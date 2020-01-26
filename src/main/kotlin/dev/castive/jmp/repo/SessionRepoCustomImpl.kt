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

package dev.castive.jmp.repo

import dev.castive.jmp.entity.Session
import dev.castive.jmp.entity.User
import dev.dcas.jmp.spring.security.model.entity.UserEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class SessionRepoCustomImpl @Autowired constructor(
	private val sessionRepo: SessionRepo,
	private val passwordEncoder: PasswordEncoder
): SessionRepoCustom {
	override fun findFirstByUserAndRefreshTokenAndActiveTrue(user: UserEntity, refreshToken: String): Session? {
		val sessions = sessionRepo.findAllByUserAndActiveIsTrue(user as User)
		return sessions.firstOrNull {
			passwordEncoder.matches(refreshToken, it.refreshToken)
		}
	}

	override fun findFirstByUserAndRequestTokenAndActiveTrue(user: UserEntity, requestToken: String): Session? {
		val sessions = sessionRepo.findAllByUserAndActiveIsTrue(user as User)
		return sessions.firstOrNull {
			passwordEncoder.matches(requestToken, it.requestToken)
		}
	}

	override fun findFirstByRequestTokenAndActiveTrue(requestToken: String): Session? {
		val sessions = sessionRepo.findAllByActiveTrue()
		return sessions.firstOrNull {
			kotlin.runCatching {
				passwordEncoder.matches(requestToken, it.requestToken)
			}.getOrDefault(false)
		}
	}
}
