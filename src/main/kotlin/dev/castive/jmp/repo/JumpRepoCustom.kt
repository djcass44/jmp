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

package dev.castive.jmp.repo

import dev.castive.jmp.entity.Jump
import dev.castive.jmp.entity.User

interface JumpRepoCustom {
	fun findAllByUser(user: User?): List<Jump>
	fun findAllByUser(user: User?, includeAliases: Boolean): List<Jump>
	fun findAllByUserAndId(user: User?, id: Int): List<Jump>
	fun searchByTerm(user: User?, term: String, exact: Boolean = true): List<Jump>
}