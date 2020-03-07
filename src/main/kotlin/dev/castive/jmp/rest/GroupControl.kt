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
import dev.castive.jmp.data.dto.CreateGroupDTO
import dev.castive.jmp.data.dto.EditGroupUsersDTO
import dev.castive.jmp.repo.GroupRepoCustom
import dev.castive.jmp.security.SecurityConstants
import dev.castive.jmp.tasks.GroupsTask
import dev.castive.jmp.util.assertUser
import dev.castive.jmp.util.broadcast
import dev.castive.log2.loga
import dev.castive.log2.loge
import dev.castive.log2.logi
import dev.dcas.jmp.security.shim.entity.Group
import dev.dcas.jmp.security.shim.repo.GroupRepo
import dev.dcas.jmp.security.shim.repo.UserRepo
import dev.dcas.util.extend.isESNullOrBlank
import dev.dcas.util.spring.responses.ForbiddenResponse
import dev.dcas.util.spring.responses.NotFoundResponse
import dev.dcas.util.spring.toPage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.transaction.Transactional

@Transactional
@RestController
@RequestMapping("/v2_1/group")
class GroupControl @Autowired constructor(
	private val groupRepo: GroupRepo,
	private val groupRepoCustom: GroupRepoCustom,
	private val userRepo: UserRepo,
	private val groupTask: GroupsTask
) {

	@PreAuthorize("hasRole('USER')")
	@GetMapping
	fun getGroups(
		@RequestParam("size", defaultValue = "20") size: Int = 20,
		@RequestParam("page", defaultValue = "0") page: Int = 0,
		@RequestParam("query", defaultValue = "") query: String = ""): Page<Group> {
			return groupRepoCustom.searchByTerm(SecurityContextHolder.getContext().assertUser(), query, false).sortedWith(
				// sort by name, then creation
				compareBy({ it.public }, { it.defaultFor }, { it.name })
			).toPage(PageRequest.of(page, size))
		}

	@PreAuthorize("hasRole('USER')")
	@PutMapping
	fun createGroup(@RequestBody group: CreateGroupDTO): Group {
		val user = SecurityContextHolder.getContext().assertUser()
		if(group.name.startsWith("_")) {
			"Blocked attempt to create reserved group: ${group.name} by ${user.username}".loga(javaClass)
			throw ForbiddenResponse("'_' group names are reserved")
		}
		val created = groupRepo.save(Group(
			UUID.randomUUID(),
			group.name,
			SecurityConstants.sourceLocal,
			group.public,
			group.defaultFor,
			mutableSetOf(user)
		))
		"Group created: ${created.name} by ${user.username}".logi(javaClass)
		FSA(FSA.EVENT_UPDATE_GROUP, null).broadcast()
		return created
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping
	fun updateGroup(@RequestBody group: Group): Group {
		val user = SecurityContextHolder.getContext().assertUser()
		val existing = groupRepo.findByIdOrNull(group.id) ?: throw NotFoundResponse(Responses.NOT_FOUND_GROUP)
		// check that user can modify group
		if(!user.isAdmin() && !existing.users.contains(user) && !existing.name.startsWith("_"))
			throw ForbiddenResponse()
		existing.apply {
			name = group.name
			if (source == SecurityConstants.sourceLocal) { // && user is admin
				public = group.public
				// we cannot have a public AND default group
				if(!public && !group.defaultFor.isESNullOrBlank())
					defaultFor = group.defaultFor
			}
		}
		"Group ${existing.id} modified by ${user.username}".logi(javaClass)
		// ask the groupstask cron to update public/default relations
		groupTask.run()
		FSA(FSA.EVENT_UPDATE_GROUP, null).broadcast()
		return groupRepo.save(existing)
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/{id}")
	fun deleteGroup(@PathVariable(value = "id", required = true) id: UUID) {
		val user = SecurityContextHolder.getContext().assertUser()
		val existing = groupRepo.findByIdOrNull(id) ?: throw NotFoundResponse(Responses.NOT_FOUND_GROUP)
		// check that user can delete group
		if(!user.isAdmin() && !existing.users.contains(user) && !existing.name.startsWith("_"))
			throw ForbiddenResponse()
		"Group ${existing.name} removed by ${user.username}".logi(javaClass)
		groupRepo.delete(existing)
		FSA(FSA.EVENT_UPDATE_GROUP, null).broadcast()
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping("/mod")
	fun modifyUsers(@RequestBody mods: EditGroupUsersDTO, @RequestParam uid: UUID) {
		val user = SecurityContextHolder.getContext().assertUser()
		val newUser = userRepo.findByIdOrNull(uid) ?: throw NotFoundResponse(Responses.NOT_FOUND_USER)
		// add user to groups
		mods.add.forEach { g ->
			groupRepo.findByIdOrNull(g)?.let {
				// check if user is admin
				if((user.isAdmin() || it.containsUser(user)) && !it.name.startsWith("_")) {
					it.users.add(newUser)
					"${user.username} added ${newUser.username} to group: ${it.name}".logi(javaClass)
					groupRepo.save(it)
				}
				else
					"Cannot add to group: ${it.name}: [${user.isAdmin()}, ${it.containsUser(user)}]".logi(javaClass)
			} ?: "[add] Failed to find group with id: $g".loge(javaClass)
		}
		mods.rm.forEach { g ->
			groupRepo.findByIdOrNull(g)?.let {
				// check if user is admin
				if((user.isAdmin() || it.containsUser(user)) && !it.name.startsWith("_")) {
					// removeIf to ensure that object.equals doesn't get in the way
					"${user.username} removed ${newUser.username} from group: ${it.name}, result: ${it.users.removeIf { (id) ->
						id == newUser.id
					}}".logi(javaClass)
					groupRepo.save(it)
				}
				else
					"Cannot remove from group: ${it.name}: [${user.isAdmin()}, ${it.containsUser(user)}]".logi(javaClass)
			} ?: "[rm] Failed to find group with id: $g".loge(javaClass)
		}
	}
}
