/*
 *    Copyright [2019 Django Cass
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

package dev.castive.jmp.db.repo

import dev.castive.jmp.db.dao.*
import dev.castive.jmp.util.asArrayList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Find all Jumps by a given location
 */
fun Jumps.findAllByLocation(location: String): List<Jump> = transaction {
	Jump.find {
		Jumps.location eq location
	}.toList()
}

fun Jumps.findAllByUser(user: User?): ArrayList<Jump> {
	val results = arrayListOf<Jump>()
	transaction {
		// get global jumps
		results.addAll(Jump.find {
			owner.isNull() and ownerGroup.isNull()
		})
		// add jumps in public groups
		Groups.findAllPublic().forEach { group ->
			Jumps.findAllByOwnerGroup(group).forEach {
				if(!results.contains(it))
					results.add(it)
			}
		}
		if(user == null)
			return@transaction
		// get jumps owned by the user
		results.addAll(Jump.find {
			owner.eq(user.id)
		})
		// get jumps in groups user is in
		Groups.findAllContainingUser(user).forEach {
			results.addAll(Jumps.findAllByOwnerGroup(it))
		}
	}
	return results
}

fun Jumps.findAllByUser(user: User?, includeAliases: Boolean): ArrayList<JumpData> {
	val jumps = findAllByUser(user)
	val results = arrayListOf<JumpData>()
	transaction {
		jumps.forEach { jmp ->
			val entity = JumpData(jmp)
			if(includeAliases) {
				results.addAll(Aliases.findAllByParent(jmp.id.value).map {
					JumpData(
						entity.id,
						it.name,
						entity.location,
						entity.personal,
						entity.owner,
						entity.image,
						entity.title,
						entity.alias,
						entity.metaCreation,
						entity.metaUpdate,
						entity.metaUsage
					)
				})
			}
			results.add(entity)
		}
	}
	return results
}

fun Jumps.findAllById(user: User?, id: Int): ArrayList<Jump> {
	val jumps = findAllByUser(user)
	return transaction {
		jumps.mapNotNull {
			if(it.id.value == id) it else null
		}
	}.asArrayList()
}

fun Jumps.findAllByOwnerGroup(group: Group): List<Jump> = transaction {
	Jump.find {
		ownerGroup eq group.id
	}.toList()
}

/**
 * Find all Jumps that match a specified search term and are visible to the given user
 */
fun Jumps.searchByTerm(user: User?, term: String, caseSensitive: Boolean = true): ArrayList<Jump> {
	val jumps = findAllByUser(user)
	val results = arrayListOf<Jump>()
	transaction {
		jumps.forEach { jmp ->
			if(jmp.name.equals(term, ignoreCase = !caseSensitive))
				results.add(jmp)
			// check aliases
			results.addAll(Aliases.findAllByParent(jmp.id.value).mapNotNull {
				if(it.name.equals(term, ignoreCase = !caseSensitive)) jmp else null
			})
		}
	}
	return results
}
