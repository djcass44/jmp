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

package dev.castive.jmp.auth

import com.amdelamar.jhash.Hash
import dev.castive.javalin_auth.auth.RequestUserLocator
import dev.castive.javalin_auth.auth.data.User2
import dev.castive.javalin_auth.auth.data.UserEntity
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.findFirstByRequestTokenAndActive
import dev.castive.jmp.db.repo.findFirstByUsername
import dev.castive.jmp.db.repo.findFirstByUsernameAndSource
import dev.castive.jmp.db.repo.new
import dev.dcas.util.extend.uuid
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class UserLocation: RequestUserLocator.UserLocation<UUID> {

	override fun createUser(user: User2): UserEntity<UUID> = Users.new(user).asUserEntity()

	override fun findActiveSession(requestToken: String): Pair<UserEntity<UUID>, String>? {
		val session = Sessions.findFirstByRequestTokenAndActive(requestToken) ?: return null
		return transaction {
			session.user.asUserEntity() to session.id.value.toString()
		}
	}

	override fun findByBasic(username: String, password: String): UserEntity<UUID>? {
		val targetUser = Users.findFirstByUsername(username) ?: return null
		// check if the users hashed password matches the hash attempted password
		return if(transaction {
				Hash.password(password.toCharArray()).verify(targetUser.hash)
			}) targetUser.asUserEntity() else null
	}

	override fun findUserById(id: String?): UserEntity<UUID>? = transaction {
		val uid = id?.uuid() ?: return@transaction null
		return@transaction User.findById(uid)?.asUserEntity()
	}

	override fun findUserByUsername(username: String, source: String): UserEntity<UUID>? = transaction {
		Users.findFirstByUsernameAndSource(username, source)?.asUserEntity()
	}
}
