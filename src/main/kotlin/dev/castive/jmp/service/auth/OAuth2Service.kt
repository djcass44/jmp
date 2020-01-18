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

package dev.castive.jmp.service.auth

import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Role
import dev.castive.jmp.entity.Session
import dev.castive.jmp.entity.User
import dev.castive.jmp.repo.MetaRepo
import dev.castive.jmp.repo.SessionRepo
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.oauth2.AbstractOAuth2Provider
import dev.castive.jmp.util.ellipsize
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logw
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OAuth2Service @Autowired constructor(
	private val userRepo: UserRepo,
	private val metaRepo: MetaRepo,
	private val sessionRepo: SessionRepo
) {
	fun createUser(accessToken: String, refreshToken: String, provider: AbstractOAuth2Provider): Boolean {
		val userData = provider.getUserInformation(accessToken)
		if(userData == null) {
			"Failed to get user information for user with token: ${accessToken.ellipsize(24)}".loge(javaClass)
			return false
		}
		// users are prepended with oauth2 to ensure there are no collisions
		val oauthUsername = "oauth2/${userData.username}"
		// only create the user if they don't exist
		if(!userRepo.existsByUsername(oauthUsername)) {
			// create the user
			val id = UUID.randomUUID()
			val meta = metaRepo.save(Meta.fromUser(id))
			val user = userRepo.save(User(
				id,
				oauthUsername,
				userData.displayName,
				userData.avatarUrl,
				null,
				mutableListOf(Role.ROLE_USER),
				meta,
				"oauth2/${provider.name}"
			))
			// create a session for the new user
			newSession(accessToken, refreshToken, user)
		}
		else {
			"User already exists: $oauthUsername".logi(javaClass)
			// create/update the session for the existing user
			val user = userRepo.findFirstByUsername(oauthUsername)?.let {
				userRepo.save(it.apply {
					displayName = userData.displayName
					avatarUrl = userData.avatarUrl
				})
			}
			newSession(accessToken, refreshToken, user)
		}
		return true
	}

	/**
	 * Create and update the sessions for the user
	 * @param user: what user we want to create the session for. This can be null if there is an active session (e.g. for refreshing)
	 */
	private fun newSession(requestToken: String, refreshToken: String, user: User?, oldToken: String = refreshToken) {
		val existingSession = sessionRepo.findFirstByRefreshTokenAndActiveTrue(oldToken)?.let {
			sessionRepo.save(it.apply {
				active = false
			})
		}

		if(user == null && existingSession == null) {
			"Unable to create session as we have no context of the user".logw(javaClass)
			return
		}
		val meta = metaRepo.save(Meta.fromUser(user ?: existingSession!!.user))
		// create the new session
		sessionRepo.save(Session(
			UUID.randomUUID(),
			requestToken,
			refreshToken,
			meta,
			user ?: existingSession!!.user,
			true
		))
	}
}
