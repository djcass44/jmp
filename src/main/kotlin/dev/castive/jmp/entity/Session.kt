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

package dev.castive.jmp.entity

import dev.castive.jmp.security.JwtTokenProvider
import dev.castive.jmp.util.UUIDConverterCompat
import dev.castive.jmp.util.hash
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "Sessions")
data class Session(
	@Id
	@Convert(converter = UUIDConverterCompat::class)
	val id: UUID,
	val requestToken: String,
	val refreshToken: String,
	@OneToOne
	val meta: Meta,
	@ManyToOne
	val user: User,
	var active: Boolean = false
) {
	companion object {
		/**
		 * Creates a session object and generates access keys
		 * Generated session contains hashed tokens for security
		 * @return triple containing the session, request token, refresh token
		 */
		fun fromUser(user: User, meta: Meta, provider: JwtTokenProvider): Triple<Session, String, String> {
			val request = provider.createRequestToken(user.username, user.roles)
			val refresh = provider.createRefreshToken(user.username, user.roles)
			val session = Session(
				UUID.randomUUID(),
				request.hash(),
				refresh.hash(),
				meta,
				user,
				true
			)
			return Triple(session, request, refresh)
		}
	}
}
