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

import dev.castive.jmp.api.Responses
import dev.castive.jmp.data.FSA
import dev.castive.jmp.data.GroupUserEditEntity
import dev.castive.jmp.entity.Group
import dev.castive.jmp.except.ForbiddenResponse
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.repo.GroupRepo
import dev.castive.jmp.repo.UserRepo
import dev.castive.jmp.service.UserService
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.broadcast
import dev.castive.log2.logi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/v2_1/group")
class GroupControl @Autowired constructor(
	private val groupRepo: GroupRepo,
	private val userRepo: UserRepo,
	private val userService: UserService,
	private val groupTask: GroupsTask
) {

	@PreAuthorize("hasRole('USER')")
	@GetMapping
	fun getGroups(): List<Group> = groupRepo.findAllByUsersIsContaining(userService.assertUser())

	@PreAuthorize("hasRole('USER')")
	@PutMapping
	fun createGroup(@RequestBody group: Group): Group {
		val user = userService.assertUser()
		val created = groupRepo.save(Group(
			UUID.randomUUID(),
			group.name,
			group.source,
			group.public,
			group.defaultFor,
			mutableSetOf(user)
		))
		FSA(FSA.EVENT_UPDATE_GROUP, null).broadcast()
		return created
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping
	fun updateGroup(@RequestBody group: Group): Group {
		val user = userService.assertUser()
		val existing = groupRepo.findByIdOrNull(group.id) ?: throw NotFoundResponse(Responses.NOT_FOUND_GROUP)
		// check that user can modify group
		if(!user.isAdmin() && !existing.users.contains(user))
			throw ForbiddenResponse()
		existing.apply {
			name = group.name
			if (source == "local") { // && user is admin
				public = group.public
				// we cannot have a public AND default group
				if(!public)
					defaultFor = group.defaultFor
			}
		}
		// ask the groupstask cron to update public/default relations
		groupTask.run()
		FSA(FSA.EVENT_UPDATE_GROUP, null).broadcast()
		return groupRepo.save(existing)
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/{id}")
	fun deleteGroup(@PathVariable(value = "id", required = true) id: UUID) {
		val user = userService.assertUser()
		val existing = groupRepo.findByIdOrNull(id) ?: throw NotFoundResponse(Responses.NOT_FOUND_GROUP)
		// check that user can delete group
		if(!user.isAdmin() && !existing.users.contains(user))
			throw ForbiddenResponse()
		"${user.username} is removing group ${existing.name}".logi(javaClass)
		groupRepo.delete(existing)
		FSA(FSA.EVENT_UPDATE_GROUP, null).broadcast()
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping("/mod")
	fun modifyUsers(@RequestBody mods: GroupUserEditEntity, @RequestParam uid: UUID) {
		val user = userService.assertUser()
		val newUser = userRepo.findByIdOrNull(uid) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		// add user to groups
		mods.add.forEach { g ->
			groupRepo.findFirstByName(g)?.let {
				// check if user is admin
				if(user.isAdmin() || it.users.contains(user)) {
					it.users.add(newUser)
				}
				groupRepo.save(it)
				"${user.username} added ${newUser.username} to group: ${it.name}".logi(javaClass)
			}
		}
		mods.rm.forEach { g ->
			groupRepo.findFirstByName(g)?.let {
				// check if user is admin
				if(user.isAdmin() || it.users.contains(user)) {
					it.users.remove(newUser)
				}
				groupRepo.save(it)
				"${user.username} removed ${newUser.username} from group: ${it.name}".logi(javaClass)
			}
		}
	}
}
