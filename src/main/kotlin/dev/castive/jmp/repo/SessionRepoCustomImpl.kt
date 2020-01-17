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

import com.amdelamar.jhash.Hash
import dev.castive.jmp.entity.Session
import dev.castive.jmp.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class SessionRepoCustomImpl @Autowired constructor(
	private val sessionRepo: SessionRepo
): SessionRepoCustom {
	override fun findFirstByUserAndRefreshTokenAndActiveTrue(user: User, refreshToken: String): Session? {
		val sessions = sessionRepo.findAllByUserAndActiveIsTrue(user)
		val hash = Hash.password(refreshToken.toCharArray())
		return sessions.firstOrNull {
			hash.verify(it.refreshToken)
		}
	}

	override fun findFirstByUserAndRequestTokenAndActiveTrue(user: User, requestToken: String): Session? {
		val sessions = sessionRepo.findAllByUserAndActiveIsTrue(user)
		val hash = Hash.password(requestToken.toCharArray())
		return sessions.firstOrNull {
			hash.verify(it.requestToken)
		}
	}
}