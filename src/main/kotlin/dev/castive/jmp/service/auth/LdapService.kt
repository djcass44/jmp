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

package dev.castive.jmp.service.auth

import dev.castive.jmp.data.BasicAuth
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Role
import dev.castive.jmp.entity.User
import dev.castive.jmp.except.ConflictResponse
import dev.castive.jmp.repo.MetaRepo
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.SecurityConstants
import dev.castive.log2.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.DirContextOperations
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.AbstractContextMapper
import org.springframework.ldap.filter.EqualsFilter
import org.springframework.ldap.query.LdapQueryBuilder
import org.springframework.ldap.query.SearchScope
import org.springframework.ldap.support.LdapUtils
import org.springframework.stereotype.Service
import java.util.UUID
import javax.annotation.PostConstruct
import javax.naming.directory.Attributes


@Service
class LdapService @Autowired constructor(
	private val metaRepo: MetaRepo,
	private val userRepo: UserRepo,
	private val ldapTemplate: LdapTemplate
): BasicAuthProvider {

	@Value("\${spring.ldap.enabled:false}")
	val enabled: Boolean = false

	override val sourceName: String
		get() = SecurityConstants.sourceLdap


	@PostConstruct
	fun init() {
		"LDAP Jwt generation enabled: $enabled".loga(javaClass)
	}

	/**
	 * Extracts an LDAP distinguished name from a uid
	 * https://docs.spring.io/spring-ldap/docs/1.3.x/reference/html/user-authentication.html
	 */
	private fun getDnForUser(uid: String): String? {
		val f = EqualsFilter("uid", uid)
		val result = ldapTemplate.search(LdapUtils.emptyLdapName(), f.toString(),
			object : AbstractContextMapper<Any>() {
				override fun doMapFromContext(ctx: DirContextOperations): Any {
					return ctx.nameInNamespace
				}
			})
		if (result.size != 1) {
			"Failed to find user with uid=$uid".logi(javaClass)
			return null
		}
		return result[0] as String
	}

	/**
	 * Search the directory for a user with a matching DN
	 * Only the first item is returned
	 */
	private fun getUser(uid: String): User? {
		"Attempting to extract user details from LDAP where uid=$uid".logv(javaClass)
		val query = LdapQueryBuilder.query()
			.searchScope(SearchScope.SUBTREE)
			.timeLimit(3_000)
			.countLimit(1)
			.base(LdapUtils.emptyLdapName())
			.where("uid").`is`(uid)
		return kotlin.runCatching {
			ldapTemplate.search(query, UserAttributeMapper(metaRepo, userRepo)).first()
		}.onFailure {
			"Failed to execute LDAPService::getUser for uid=$uid".loge(javaClass, it)
		}.getOrNull()
	}

	override fun getUserByName(basicAuth: BasicAuth): User? {
		if(!enabled)
			return null
		val (username, password) = basicAuth
		// get the users DN from their username
		val dn = getDnForUser(username) ?: return null
		val ctx = kotlin.runCatching {
			// check that we are able to bind using the users credentials
			ldapTemplate.contextSource.getContext(dn, password)
		}.onFailure {
			"Failed to bind LDAP using DN: $dn".loge(javaClass, it)
		}.getOrNull()
		// context must be close
		if(ctx == null)
			return null
		else
			LdapUtils.closeContext(ctx)
		"Successfully created LDAP binding to $dn".logd(javaClass)
		return getUser(username)
	}

	/**
	 * Converts LDAP attributes into a JMP user
	 */
	private class UserAttributeMapper(
		private val metaRepo: MetaRepo,
		private val userRepo: UserRepo
	): AttributesMapper<User> {
		override fun mapFromAttributes(attr: Attributes): User {
			val username = attr.get("uid").get() as String
			// if there is an existing user, return them
			userRepo.findFirstByUsernameAndSource(username, SecurityConstants.sourceLdap)?.let {
				"Found existing match for LDAP user: $username".logv(javaClass)
				return it
			}
			// we cannot have duplicate usernames
			if(userRepo.existsByUsername(username)) {
				"Blocking possible merger of new ldap user and existing user: $username".logw(javaClass)
				throw ConflictResponse("Username is already in use")
			}
			// otherwise, create a new user
			val id = UUID.randomUUID()
			val meta = metaRepo.save(Meta.fromUser(id))
			"Creating new user representation for LDAP user: $username ($id)".logi(javaClass)
			return userRepo.save(User(
				id,
				username,
				attr.get("cn").get() as String,
				null,
				null,
				mutableListOf(Role.ROLE_USER),
				meta,
				SecurityConstants.sourceLdap
			))
		}
	}
}
