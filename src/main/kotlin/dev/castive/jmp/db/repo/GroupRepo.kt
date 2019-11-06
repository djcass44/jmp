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
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Find all Groups with a given name
 */
fun Groups.findAllByName(name: String): List<Group> = transaction {
	Group.find {
		Groups.name eq name
	}.toList()
}

/**
 * Find all Groups that ARE NOT from a given provider
 */
fun Groups.findAllNotFrom(from: String): List<Group> = transaction {
	Group.find {
		Groups.from neq from
	}.toList()
}

/**
 * Find all groups that a given has is a member of
 */
fun Groups.findAllContainingUser(user: User): List<Group> = transaction {
	Group.wrapRows(
		(Groups innerJoin GroupUsers innerJoin Users)
			.slice(columns)
			.select { Users.id eq user.id }
			.withDistinct()
	).toList()
}