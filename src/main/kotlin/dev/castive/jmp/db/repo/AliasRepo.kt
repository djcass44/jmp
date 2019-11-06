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

import dev.castive.jmp.db.dao.Alias
import dev.castive.jmp.db.dao.Aliases
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Find all aliases by their parent Jump id
 */
fun Aliases.findAllByParent(parent: Int): List<Alias> = transaction {
	Alias.find {
		Aliases.parent eq parent
	}.toList()
}