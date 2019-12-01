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

package dev.castive.jmp.service

import dev.castive.jmp.entity.User
import dev.castive.jmp.except.UnauthorizedResponse
import dev.castive.jmp.repo.UserRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class UserService @Autowired constructor(
	private val userRepo: UserRepo
) {
	fun assertUser(): User = getUser() ?: throw UnauthorizedResponse()
	
	fun getUser(): User? {
		val username = SecurityContextHolder.getContext().authentication.name
		return userRepo.findFirstByUsername(username)
	}
}
