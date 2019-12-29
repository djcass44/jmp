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
import dev.castive.jmp.data.JumpDTO
import dev.castive.jmp.data.dto.DoJumpDTO
import dev.castive.jmp.data.dto.EditJumpDTO
import dev.castive.jmp.entity.Alias
import dev.castive.jmp.entity.Jump
import dev.castive.jmp.entity.Meta
import dev.castive.jmp.except.BadRequestResponse
import dev.castive.jmp.except.ConflictResponse
import dev.castive.jmp.except.ForbiddenResponse
import dev.castive.jmp.except.NotFoundResponse
import dev.castive.jmp.repo.*
import dev.castive.jmp.service.MetadataService
import dev.castive.jmp.service.OwnerService
import dev.castive.jmp.util.assertUser
import dev.castive.jmp.util.broadcast
import dev.castive.jmp.util.toPage
import dev.castive.jmp.util.user
import dev.castive.log2.logi
import dev.castive.log2.logv
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.transaction.Transactional

@Transactional
@RestController
@RequestMapping("/v2/jump")
class JumpControl @Autowired constructor(
	private val metadata: MetadataService,
	private val jumpRepo: JumpRepo,
	private val jumpRepoCustom: JumpRepoCustom,
	private val groupRepo: GroupRepo,
	private val metaRepo: MetaRepo,
	private val aliasRepo: AliasRepo,
	private val ownerService: OwnerService
) {
	@GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getAll(
		@RequestParam("size", defaultValue = "20") size: Int = 20,
		@RequestParam("page", defaultValue = "0") page: Int = 0,
		@RequestParam("query", defaultValue = "") query: String = ""): Page<JumpDTO> {
		return jumpRepoCustom.searchByTerm(SecurityContextHolder.getContext().user(), query, exact = false).map {
			ownerService.getDTO(it)
		}.toPage(PageRequest.of(page, size))
	}

	@GetMapping("/{target}")
	fun jumpTo(@PathVariable target: String, @RequestParam(required = false) id: Int?): DoJumpDTO {
		val user = SecurityContextHolder.getContext().user()
		if(target.isBlank() && id == null) {
			throw BadRequestResponse("Empty or null target: $target, id: $id")
		}
		val jumps = if(id != null) {
			"Got request for specific jump: $id".logi(javaClass)
			jumpRepo.findAllById(listOf(id))
		}
		else
			jumpRepoCustom.searchByTerm(user, target)
		"Got ${jumps.size} as a response to $target, $id".logv(javaClass)
		val res = run {
			val found = jumps.size == 1
			return@run if(found) {
				// increment the usage counter
				jumpRepo.save(jumps[0].apply { usage++ })
				true to jumps[0].location
			}
			else
				false to "/similar?query=$target"
		}
		return DoJumpDTO(res.first, res.second)
	}

	@PreAuthorize("hasRole('USER')")
	@PutMapping
	fun createJump(
		@RequestBody add: EditJumpDTO,
		@RequestParam(required = false) gid: UUID?
	): Jump {
		val user = SecurityContextHolder.getContext().assertUser()
		// block non-admin users from creating global jumps
		if(add.personal == Jump.TYPE_GLOBAL && !user.isAdmin())
			throw ForbiddenResponse("Only an admin can create global Jumps")
		if(jumpRepo.existsById(add.id))
			throw ConflictResponse()
		val group = gid?.let {
			groupRepo.findByIdOrNull(it)
		}
		val created = jumpRepo.save(Jump(
			0,
			add.name,
			add.location,
			null,
			mutableSetOf(),
			if(add.personal == Jump.TYPE_PERSONAL) user else null,
			group,
			null,
			metaRepo.save(Meta.fromUser(user))
		))
		add.alias.forEach {
			aliasRepo.save(Alias(0, it.name, created.id))
		}
		// get additional metadata
		metadata.getImage(created.location)
		metadata.getTitle(created.location)
		FSA(FSA.EVENT_UPDATE_JUMP, null).broadcast()
		return created
	}

	@PreAuthorize("hasRole('USER')")
	@PatchMapping
	fun updateJump(@RequestBody update: EditJumpDTO): ResponseEntity<*> {
		val user = SecurityContextHolder.getContext().assertUser()
		val existing = jumpRepo.findByIdOrNull(update.id) ?: throw NotFoundResponse(Responses.NOT_FOUND_JUMP)
		// check if user is allowed to modify the jump
		if(existing.owner != user && !user.isAdmin() && (jumpRepoCustom.findAllByUserAndId(user, existing.id).isEmpty() || existing.isPublic()))
			throw ForbiddenResponse()
		metaRepo.save(existing.meta.onUpdate(user))
		// add aliases
		val updated = arrayListOf<Alias>()
		update.alias.forEach {
			// create the alias if it doesn't exist
			if(it.id == null || it.id == 0) {
				val a = Alias(0, it.name, existing.id)
				updated.add(a)
				existing.alias.add(a)
			}
			else {
				aliasRepo.findByIdOrNull(it.id)?.let { a ->
					updated.add(a.apply { name = a.name })
				}
			}
		}
		// batch-save
		aliasRepo.saveAll(updated)
		// remove dangling aliases
		val dangling = aliasRepo.findAllByParent(existing.id)
		aliasRepo.deleteAll(dangling.filter {
			!updated.contains(it)
		})
		// save changes to jump
		val jump = jumpRepo.save(existing.apply {
			name = update.name
			location = update.location
		})
		// update metadata
		metadata.getImage(jump.location)
		metadata.getTitle(jump.location)
		FSA(FSA.EVENT_UPDATE_JUMP, null).broadcast()
		return ResponseEntity.ok(jump.id)
	}

	@PreAuthorize("hasRole('USER')")
	@DeleteMapping("/{id}")
	fun deleteJump(@PathVariable id: Int): ResponseEntity<Nothing> {
		val user = SecurityContextHolder.getContext().assertUser()
		val jump = jumpRepo.findByIdOrNull(id) ?: throw NotFoundResponse(Responses.NOT_FOUND_JUMP)
		// check if jump is global and user is admin
		if(jump.isPublic() && !user.isAdmin())
			throw ForbiddenResponse()
		if(jump.owner != null && jump.owner != user)
			throw ForbiddenResponse()
		// check that the user is part of the group owning the jump
		if(jump.ownerGroup != null) {
			val groups = groupRepo.findAllByUsersIsContaining(user).filter {
				it == jump.ownerGroup
			}
			if (groups.isEmpty() && !user.isAdmin())
				throw ForbiddenResponse()
		}
		// delete aliases
		aliasRepo.deleteAllByParent(jump.id)
		jumpRepo.delete(jump)
		FSA(FSA.EVENT_UPDATE_JUMP, null).broadcast()

		return ResponseEntity.noContent().build<Nothing>()
	}
}
