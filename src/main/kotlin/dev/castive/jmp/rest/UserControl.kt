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
import dev.castive.jmp.api.Responses
import dev.castive.jmp.data.BasicAuth
import dev.castive.jmp.data.FSA
import dev.castive.jmp.entity.Group
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.entity.Role
import dev.castive.jmp.entity.User
import dev.castive.jmp.except.BadRequestResponse
import dev.castive.jmp.except.ForbiddenResponse
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.repo.GroupRepo
import dev.castive.jmp.repo.MetaRepo
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.security.SecurityConstants
import dev.castive.jmp.service.UserService
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.broadcast
import dev.castive.jmp.util.hash
import dev.castive.log2.loga
import dev.castive.log2.logw
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/v2/user")
class UserControl @Autowired constructor(
	private val userRepo: UserRepo,
	private val groupRepo: GroupRepo,
	private val metaRepo: MetaRepo,
	private val userService: UserService,
	private val groupTask: GroupsTask
) {
	private val limiter = RateLimiter.create(5.0)

	@PreAuthorize("hasRole('USER')")
	@GetMapping
	fun getUsers(): List<User> {
		return userRepo.findAll().toList()
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/me")
	fun getCurrentUser(): User? = userService.getUser()

	@PutMapping
	fun createUser(@RequestBody basicAuth: BasicAuth): ResponseEntity<String> {
		if(limiter.tryAcquire()) {
			val id = UUID.randomUUID()
			val meta = metaRepo.save(Meta.fromUser(id))
			val created = userRepo.save(
				User(
					UUID.randomUUID(),
					basicAuth.username,
					"",
					null,
					basicAuth.password.hash(),
					mutableListOf(Role.ROLE_USER),
					meta,
					SecurityConstants.sourceLocal
				)
			)
			// ask the groupstask cron to update public/default relations
			groupTask.run()
			FSA(FSA.EVENT_UPDATE_USER, null).broadcast()
			return ResponseEntity(created.username, HttpStatus.CREATED)
		}
		else
			return ResponseEntity(Responses.GENERIC_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS)
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping
	fun patchUser(@RequestParam uid: UUID, @RequestParam admin: Boolean) {
		val user = userService.assertUser()
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
		FSA(FSA.EVENT_UPDATE_USER, null).broadcast()
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
		FSA(FSA.EVENT_UPDATE_USER, null).broadcast()
		return true
	}

	@PreAuthorize("hasRole('USER')")
	@GetMapping("/groups")
	fun getUserGroups(@RequestParam uid: UUID): List<Group> {
		val user = userRepo.findByIdOrNull(uid) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		return groupRepo.findAllByUsersIsContaining(user)
	}
}
