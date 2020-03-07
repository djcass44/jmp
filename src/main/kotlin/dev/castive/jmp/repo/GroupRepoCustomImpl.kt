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

package dev.castive.jmp.repo

import dev.castive.log2.logv
import dev.dcas.jmp.security.shim.entity.Group
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.jmp.security.shim.repo.GroupRepo
import org.springframework.stereotype.Repository

@Repository
class GroupRepoCustomImpl constructor(
	private val groupRepo: GroupRepo
): GroupRepoCustom {

	override fun searchByTerm(user: User, term: String, exact: Boolean): List<Group> {
		val groups = groupRepo.findAllByUsersIsContaining(user)
		if(term.isBlank()) {
			"Searching using blank search term, backing out".logv(javaClass)
			return groups
		}
		val results = arrayListOf<Group>()
		return if(exact) {
			groups.forEach {
				// get users with an exact name match
				if(it.name.equals(term, ignoreCase = true))
					results.add(it)
			}
			results
		}
		else {
			groups.forEach {
				// find users where name contains the search term
				if(it.name.contains(term))
					results.add(it)
			}
			results
		}
	}
}
