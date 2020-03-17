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

import dev.castive.jmp.prop.AppMetadataProps
import dev.castive.jmp.repo.JumpRepo
import dev.castive.jmp.util.isVisibleTo
import dev.castive.jmp.util.user
import dev.dcas.util.extend.safe
import dev.dcas.util.spring.responses.NotFoundResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RequestMapping("/meta")
@RestController
class MetadataControl(
	private val jumpRepo: JumpRepo,
	private val metadataProps: AppMetadataProps
) {

	@GetMapping("/icon")
	fun getIcon(@RequestParam id: Int, response: HttpServletResponse) {
		val user = SecurityContextHolder.getContext().user()
		val targetJump = jumpRepo.findByIdOrNull(id) ?: throw NotFoundResponse()
		// check whether the user is allowed to see the jump
		if(!targetJump.isVisibleTo(user))
			throw NotFoundResponse() // should this be forbidden?
		response.sendRedirect("${metadataProps.icon.url}/icon?site=${targetJump.location.safe()}")
	}
}
