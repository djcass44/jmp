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

package dev.castive.jmp.component

import dev.castive.jmp.entity.Group
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Role
import dev.castive.jmp.entity.User
import dev.castive.jmp.repo.GroupRepo
import dev.castive.jmp.repo.MetaRepo
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.SecurityConstants
import dev.castive.log2.loga
import dev.castive.log2.logf
import dev.castive.log2.logi
import dev.castive.log2.logs
import dev.dcas.util.extend.hash
import dev.dcas.util.extend.randomString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class StartupChecks @Autowired constructor(
	private val userRepo: UserRepo,
	private val metaRepo: MetaRepo,
	private val groupRepo: GroupRepo
) {
	@Value("\${spring.ldap.enabled:false}")
	private val ldapEnabled: Boolean = false

	@PostConstruct
	fun ensureAdmin() {
		val admin = userRepo.findFirstByUsername("admin")
		if (admin == null) {
			val password = 32.randomString()
			val id = UUID.randomUUID()
			val user = userRepo.save(User(id, "admin", "Admin", "", password.hash(), mutableListOf(Role.ROLE_ADMIN, Role.ROLE_USER), metaRepo.save(Meta.fromUser(id)), SecurityConstants.sourceLocal))
			"Created an admin with password: $password, id: ${user.id}".logs(javaClass)
			kotlin.runCatching {
				File("initialAdminPassword").writeText(password)
			}.onFailure {
				"Unable to write 'initialAdminPassword', password may not persist".logf(javaClass, it)
			}
		}
		else {
			val adminIsAdmin = admin.roles.contains(Role.ROLE_ADMIN)
			if(!adminIsAdmin) {
				"Restored admin permissions to 'admin' user".loga(javaClass)
				// make the admin user an admin
				userRepo.save(admin.apply {
					roles.add(Role.ROLE_ADMIN)
				})
			}
		}
	}

	@PostConstruct
	fun ensureLocalGroup() {
		groupRepo.findFirstByName("_${SecurityConstants.sourceLocal}") ?: run {
			groupRepo.save(Group(UUID.randomUUID(), "_${SecurityConstants.sourceLocal}", SecurityConstants.sourceLocal, false, SecurityConstants.sourceLocal))
			"Created 'local' default group".logi(javaClass)
		}
	}

	@PostConstruct
	fun ensureLdapGroup() {
		if(!ldapEnabled)
			return
		groupRepo.findFirstByName("_${SecurityConstants.sourceLdap}") ?: run {
			groupRepo.save(Group(UUID.randomUUID(), "_${SecurityConstants.sourceLdap}", SecurityConstants.sourceLdap, false, SecurityConstants.sourceLdap))
			"Created 'ldap' default group".logi(javaClass)
		}
	}
}
