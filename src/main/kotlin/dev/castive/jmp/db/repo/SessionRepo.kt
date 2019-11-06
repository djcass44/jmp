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

import dev.castive.jmp.db.dao.Session
import dev.castive.jmp.db.dao.Sessions
import dev.castive.jmp.db.dao.User
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

fun Sessions.findFirstByUserAndRefreshTokenAndActive(user: User, refreshToken: String): Session? = transaction {
	Session.find {
		// Find ACTIVE matching sessions
		Sessions.user eq user.id and (Sessions.refreshToken eq refreshToken) and (active eq true)
	}.limit(1).elementAtOrNull(0)
}
fun Sessions.findFirstByRefreshTokenAndActive(refreshToken: String): Session? = transaction {
	Session.find {
		Sessions.refreshToken eq refreshToken and (Sessions.active eq true)
	}.limit(1).elementAtOrNull(0)
}
fun Sessions.findFirstBySsoToken(ssoToken: String): Session? = transaction {
	Session.find {
		Sessions.ssoToken eq ssoToken
	}.elementAtOrNull(0)
}