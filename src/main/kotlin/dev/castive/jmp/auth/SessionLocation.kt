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

import dev.castive.javalin_auth.api.OAuth2
import dev.castive.javalin_auth.auth.data.UserEntity
import dev.castive.javalin_auth.auth.external.Session
import dev.castive.javalin_auth.util.SigningKey
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.Users
import dev.castive.jmp.db.repo.findFirstByEntity
import dev.castive.jmp.db.repo.findFirstByRefreshTokenAndActive
import dev.castive.log2.logi
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import dev.castive.jmp.db.dao.Session as SessionDao

class SessionLocation : Session<UUID> {
	override fun createSession(user: UserEntity<UUID>): OAuth2.TokenResponse? {
		val owner = Users.findFirstByEntity(user) ?: kotlin.run {
			"Unable to find user with id: ${user.id}".logi(javaClass)
			return null
		}
		val request = SigningKey.jwtHelper.createRequestToken(user)
		val refresh = SigningKey.jwtHelper.createRefreshToken(user)
		transaction {
			SessionDao.new {
				this.requestToken = request
				this.refreshToken = refresh
				// We MUST have one or the other
				this.user = owner
				ssoToken = null
				active = true
			}
		}
		return OAuth2.TokenResponse(request, refresh, user.source)
	}

	override fun disableSessions(token: String) {
		transaction {
			Sessions.findFirstByRefreshTokenAndActive(token)?.active = false
		}
	}

	override fun getForRefresh(refresh: String): UserEntity<UUID>? = transaction {
		Sessions.findFirstByRefreshTokenAndActive(refresh)?.apply {
			active = false
		}?.user?.asUserEntity()
	}
}
