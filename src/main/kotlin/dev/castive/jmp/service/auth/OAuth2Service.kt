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

import dev.castive.jmp.api.Responses
import dev.castive.jmp.entity.*
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.repo.*
import dev.castive.jmp.security.oauth2.AbstractOAuth2Provider
import dev.castive.log2.*
import dev.dcas.util.cache.TimedCache
import dev.dcas.util.extend.ellipsize
import dev.dcas.util.extend.hash
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.util.UUID
import javax.annotation.PostConstruct

@Service
class OAuth2Service @Autowired constructor(
	private val providers: List<AbstractOAuth2Provider>,
	private val userRepo: UserRepo,
	private val metaRepo: MetaRepo,
	private val groupRepo: GroupRepo,
	private val sessionRepo: SessionRepo,
	private val sessionRepoCustom: SessionRepoCustom
) {
	// hold tokens for 60 seconds (tick every 10)
	private val tokenCache = TimedCache<String, String>(6, tickDelay = 10_000L)
	private var counter = 0


	/**
	 * Checks whether an OAuth2 token is valid or was valid recently
	 * Reduces the amount of load put against the oauth2 provider by caching tokens for 60 seconds
	 * @return true if token is was found in the cache or is actually valid
	 */
	fun isTokenValid(token: String, provider: AbstractOAuth2Provider): Boolean {
		counter++
		if(counter > 25) {
			"Token cache contains ${tokenCache.size()} elements".logd(javaClass)
			counter = 0
		}

		val cached = tokenCache[token]
		if(cached != null)
			return true
		if(provider.isTokenValid(token)) {
			tokenCache[token] = token
			return true
		}
		return false
	}


	@PostConstruct
	fun init() {
		"Found ${providers.size} oauth2 provider(s): [${providers.joinToString(", ") { it.name }}]".logi(javaClass)
		providers.forEach {
			groupRepo.findFirstByName("_oauth2/${it.name}") ?: groupRepo.save(Group(
				// create the group if it doesn't exist
				name = "_oauth2/${it.name}",
				source = it.name,
				defaultFor = "oauth2/${it.name}"
			))
		}
		"Finished oauth2 group checks".logv(javaClass)
	}

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
			requestToken.hash(),
			refreshToken.hash(),
			meta,
			user ?: existingSession!!.user,
			true
		))
	}

	fun getAuthentication(token: String): Authentication? {
		val session = sessionRepoCustom.findFirstByRequestTokenAndActiveTrue(token) ?: return null
		val user = dev.castive.jmp.security.User(session.user)
		return UsernamePasswordAuthenticationToken(user, "", user.authorities)
	}

	/**
	 * Find an OAuth2 provider by its common name (e.g. 'github', 'google')
	 * @param name: the name of the provider
	 * @return AbstractOAuth2Provider or throw http 404 if it can't be found
	 */
	fun findProviderByNameOrThrow(name: String): AbstractOAuth2Provider = findProviderByName(name) ?: throw NotFoundResponse(
		Responses.NOT_FOUND_PROVIDER)

	/**
	 * Find an OAuth2 provider by its common name (e.g. 'github', 'google')
	 * @param name: the name of the provider
	 * @return AbstractOAuth2Provider or null if it can't be found
	 */
	fun findProviderByName(name: String): AbstractOAuth2Provider? = providers.firstOrNull {
		it.name == name
	}
}
