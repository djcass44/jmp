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

package dev.castive.jmp.rest

import dev.castive.jmp.data.dto.JumpDTO
import dev.castive.jmp.repo.JumpEventRepo
import dev.castive.jmp.repo.JumpRepo
import dev.castive.jmp.service.OwnerService
import dev.castive.log2.logv
import dev.dcas.jmp.security.shim.util.assertUser
import dev.dcas.util.spring.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/analytics/jump")
class JumpEventControl(
	private val jumpEventRepo: JumpEventRepo,
	private val jumpRepo: JumpRepo,
	private val ownerService: OwnerService
) {
	@PreAuthorize("hasRole('USER')")
	@GetMapping("/top", produces = [MediaType.APPLICATION_JSON_VALUE])
	fun getTopPicks(
		amount: Int = 2
	): Page<JumpDTO> {
		val user = SecurityContextHolder.getContext().assertUser()
		// get distinct jumps for this user
		val jumpsForUser = jumpEventRepo.findAllByUserId(user.id).distinctBy { it.jumpId }
		"Found ${jumpsForUser.size} unique jump events for ${user.id}".logv(javaClass)
		// get the frequency of those jumps
		val topPicks = jumpsForUser.map {
			val c = jumpEventRepo.countAllByJumpIdAndUserId(it.jumpId, it.userId)
			it to c
		}.sortedBy { -it.second }
		"Loaded ${topPicks.size} top picks for ${user.username}".logv(javaClass)
		// ensure we don't oob
		val count = amount.coerceAtMost(topPicks.size)
		return topPicks.subList(0, count).mapNotNull {
			// convert non-null jumps into the DTO form
			jumpRepo.findByIdOrNull(it.first.jumpId)?.let { j ->
				ownerService.getDTO(j)
			}
		}.toPage(PageRequest.of(0, count.coerceAtLeast(1)))
	}
}
