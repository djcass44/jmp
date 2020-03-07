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
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.jmp.security.shim.repo.UserRepo
import org.springframework.stereotype.Repository

@Repository
class UserRepoCustomImpl constructor(
	private val userRepo: UserRepo
): UserRepoCustom {

	override fun searchByTerm(term: String, exact: Boolean): List<User> {
		val users = userRepo.findAll()
		if(term.isBlank()) {
			"Searching using blank search term, backing out".logv(javaClass)
			return users
		}
		val results = arrayListOf<User>()
		return if(exact) {
			users.forEach {
				// get users with an exact username match
				if(it.username.equals(term, ignoreCase = true))
					results.add(it)
			}
			results
		}
		else {
			users.forEach {
				// find users where username/displayname contains the search term
				if(it.username.contains(term) || it.displayName.contains(term))
					results.add(it)
			}
			results
		}
	}
}
