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

import com.google.common.util.concurrent.RateLimiter
import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.data.BasicAuth
import dev.castive.jmp.data.FSA
import dev.castive.jmp.data.dto.GetUserDTO
import dev.castive.jmp.entity.Role
import dev.castive.jmp.repo.UserRepoCustom
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.Responses
import dev.castive.log2.loga
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.castive.log2.logw
import dev.dcas.jmp.security.shim.entity.Group
import dev.dcas.jmp.security.shim.entity.Meta
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.jmp.security.shim.repo.GroupRepo
import dev.dcas.jmp.security.shim.repo.MetaRepo
import dev.dcas.jmp.security.shim.repo.UserRepo
import dev.dcas.jmp.security.shim.util.assertUser
import dev.dcas.jmp.security.shim.util.user
import dev.dcas.jmp.spring.security.SecurityConstants
import dev.dcas.util.spring.responses.BadRequestResponse
import dev.dcas.util.spring.responses.ConflictResponse
import dev.dcas.util.spring.responses.ForbiddenResponse
import dev.dcas.util.spring.responses.NotFoundResponse
import dev.dcas.util.spring.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/v2/user")
class UserControl(
	private val userRepo: UserRepo,
	private val userRepoCustom: UserRepoCustom,
	private val groupRepo: GroupRepo,
	private val metaRepo: MetaRepo,
	private val groupTask: GroupsTask,
	private val passwordEncoder: PasswordEncoder,
	private val socketHandler: SocketHandler
) {
	private val limiter = RateLimiter.create(5.0)

	@PreAuthorize("hasRole('USER')")
	@GetMapping
	fun getUsers(
		@RequestParam("size", defaultValue = "20") size: Int = 20,
		@RequestParam("page", defaultValue = "0") page: Int = 0,
		@RequestParam("query", defaultValue = "") query: String = ""): Page<GetUserDTO> {
		val user = SecurityContextHolder.getContext().assertUser()
		return userRepoCustom.searchByTerm(query, false).sortedWith(
			// sort by username, then display name
			compareBy({ it.username }, { it.displayName })
		).map {
			// hide user displayname/avatar if not in the same 'source'
			GetUserDTO(it, user)
		}.toPage(PageRequest.of(page, size))
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/me")
	fun getCurrentUser(): User? = SecurityContextHolder.getContext().user()

	@PutMapping
	fun createUser(@RequestBody basicAuth: BasicAuth): ResponseEntity<String> {
		if(limiter.tryAcquire()) {
			val id = UUID.randomUUID()
			val meta = metaRepo.save(Meta.fromUser(id))
			// block the creation of duplicate users
			if(!userRepo.existsByUsername(basicAuth.username)) {
				"Failed to create user with username: ${basicAuth.username} (already exists)".loge(javaClass)
				throw ConflictResponse()
			}
			val created = userRepo.save(
				User(
					id,
					basicAuth.username,
					"",
					null,
					passwordEncoder.encode(basicAuth.password),
					mutableListOf(Role.ROLE_USER),
					meta,
					SecurityConstants.sourceLocal
				)
			)
			metaRepo.save(created.meta)
			"Created user ${created.username} with id: ${created.id}".logi(javaClass)
			// ask the groupstask cron to update public/default relations
			groupTask.run()
			socketHandler.broadcast(FSA(FSA.EVENT_UPDATE_USER, null))
			return ResponseEntity(created.username, HttpStatus.CREATED)
		}
		else
			return ResponseEntity(Responses.GENERIC_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS)
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping
	fun patchUser(@RequestParam uid: UUID, @RequestParam admin: Boolean) {
		val user = SecurityContextHolder.getContext().assertUser()
		val targetUser = userRepo.findByIdOrNull(uid) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		// block removing the superuser from admin
		if(targetUser.username == "admin")
			throw ForbiddenResponse()
		// block the user from modifying their own permissions
		if(targetUser.username == user.username)
			throw ForbiddenResponse()
		"${user.username} is updating role for ${targetUser.username}, admin: $admin".logw(javaClass)
		if(admin)
			targetUser.addRole(Role.ROLE_ADMIN)
		else
			targetUser.roles.remove(Role.ROLE_ADMIN)
		socketHandler.broadcast(FSA(FSA.EVENT_UPDATE_USER, null))
		userRepo.save(targetUser)
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{id}")
	fun deleteUser(@PathVariable(value = "id", required = true) id: UUID): Boolean {
		"Deleting user with id: $id".loga(javaClass)
		kotlin.runCatching {
			userRepo.deleteById(id)
		}.onFailure {
			throw BadRequestResponse()
		}
		socketHandler.broadcast(FSA(FSA.EVENT_UPDATE_USER, null))
		return true
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/groups")
	fun getUserGroups(@RequestParam uid: UUID): List<Group> {
		val user = userRepo.findByIdOrNull(uid) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		return groupRepo.findAllByUsersIsContaining(user)
	}
}
