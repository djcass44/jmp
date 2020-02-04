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
import dev.castive.jmp.service.SimilarityService
import dev.castive.log2.logv
import dev.dcas.jmp.security.shim.entity.User
import dev.dcas.jmp.security.shim.repo.GroupRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class JumpRepoCustomImpl @Autowired constructor(
	private val jumpRepo: JumpRepo,
	private val groupRepo: GroupRepo,
	private val aliasRepo: AliasRepo,
	private val similar: SimilarityService
): JumpRepoCustom {

	override fun findAllByUser(user: User?): List<Jump> {
		val results = mutableSetOf<Jump>()
		// add all public jumps
		results.addAll(jumpRepo.findAllByOwnerIsNullAndOwnerGroupIsNull())
		// add jumps in public groups
		groupRepo.findAllByPublicIsTrue().forEach { g ->
			jumpRepo.findAllByOwnerGroup(g).forEach {
				results.add(it)
			}
		}
		// stop if we've got no user
		if(user == null)
			return results.toList()
		// get personal jumps
		results.addAll(jumpRepo.findAllByOwner(user))
		// get jumps in groups that the user is in
		groupRepo.findAllByUsersIsContaining(user).forEach {
			results.addAll(jumpRepo.findAllByOwnerGroup(it))
		}
		return results.toList()
	}

	override fun findAllByUser(user: User?, includeAliases: Boolean): List<Jump> {
		val jumps = findAllByUser(user)
		val results = arrayListOf<Jump>()
		if(!includeAliases)
			return jumps
		jumps.forEach {
			results.addAll(aliasRepo.findAllByParent(it.id).map { alias ->
				Jump(
					it.id,
					alias.name,
					it.location,
					it.title,
					mutableSetOf(),
					it.owner,
					it.ownerGroup,
					it.image,
					it.meta,
					it.usage
				)
			})
			results.add(it)
		}
		return results
	}

	override fun findAllByUserAndId(user: User?, id: Int): List<Jump> {
		val jumps = findAllByUser(user)
		return jumps.filter {
			it.id == id
		}
	}

	override fun searchByTerm(user: User?, term: String, exact: Boolean): List<Jump> {
		val jumps = findAllByUser(user)
		// if the search is empty, don't bother doing any further checks
		if(term.isBlank()) {
			"Searching using blank search term, backing out (user: ${user?.username})".logv(javaClass)
			return jumps
		}
		val results = arrayListOf<Jump>()
		return if(exact) {
			jumps.forEach {
				if (it.name.equals(term, ignoreCase = true))
					results.add(it)
				// check aliases
				results.addAll(aliasRepo.findAllByParent(it.id).mapNotNull { alias ->
					if (alias.name.equals(term, ignoreCase = true)) it else null
				})
			}
			results
		}
		else {
			similar.forSearch(jumps, term)
		}
	}
}
