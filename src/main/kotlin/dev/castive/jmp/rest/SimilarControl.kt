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

import dev.castive.jmp.repo.JumpRepoCustom
import dev.castive.jmp.service.SimilarityService
import dev.dcas.jmp.security.shim.util.user
import dev.dcas.util.extend.asArrayList
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v2/similar")
class SimilarControl(
	private val jumpRepoCustom: JumpRepoCustom,
	private val similar: SimilarityService
) {

	@GetMapping("/{query}")
	fun checkSimilarity(@PathVariable query: String, @RequestParam(required = false) suggest: Boolean = false): List<*> {
		val user = SecurityContextHolder.getContext().user()
		val userJumps = jumpRepoCustom.findAllByUser(user, true).asArrayList()
		return if(suggest)
			similar.forSuggesting(userJumps, query)
		else
			similar.forJumping(userJumps, query)
	}

}
