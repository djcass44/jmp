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

package dev.castive.jmp.security

import dev.castive.jmp.entity.Role
import dev.castive.jmp.prop.JwtProps
import dev.castive.jmp.util.ellipsize
import dev.castive.log2.loge
import dev.dcas.util.extend.base64
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.Date
import java.util.Objects
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

@Component
class JwtTokenProvider @Autowired constructor(
	private val jwtProps: JwtProps,
	private val userDetails: UserDetails
) {
	private lateinit var secretKey: String

	@PostConstruct
	internal fun init() {
		secretKey = jwtProps.secretKey.base64()
	}

	fun createRequestToken(username: String, roles: List<Role>) = createToken(username, roles, jwtProps.requestLimit)
	fun createRefreshToken(username: String, roles: List<Role>) = createToken(username, roles, jwtProps.refreshLimit)

	private fun createToken(username: String, roles: List<Role>, limit: Long): String {
		val claims = Jwts.claims().setSubject(username)
		claims["auth"] = roles.map {
			SimpleGrantedAuthority(it.authority)
		}.filter {
			Objects.nonNull(it)
		}
		val now = Date()
		val exp = Date(now.time + limit)

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(exp)
			.signWith(SignatureAlgorithm.HS256, secretKey)
			.compact()
	}

	fun getAuthentication(token: String): Authentication? {
		val user = userDetails.loadUserByUsername(getUsername(token) ?: return null)
		return UsernamePasswordAuthenticationToken(user, "", user.authorities)
	}

	fun getUsername(token: String): String? = kotlin.runCatching {
		Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).body.subject
	}.onFailure {
		"Encountered expired or invalid Jwt token: ${token.ellipsize(24)}".loge(javaClass)
	}.getOrNull()

	fun resolveToken(request: HttpServletRequest): String? {
		val bearerToken = request.getHeader(SecurityConstants.authHeader.toLowerCase()) ?: return null
		if(bearerToken.startsWith("Bearer "))
			return bearerToken.substring(7)
		return null
	}

	fun validateToken(token: String): Boolean = kotlin.runCatching {
		Jwts.parser()
			.setSigningKey(secretKey)
			.setAllowedClockSkewSeconds(jwtProps.leeway / 1000)
			.parseClaimsJws(token)
		true
	}.onFailure {
		"Encountered expired or invalid Jwt token: ${token.ellipsize(24)}".loge(javaClass)
	}.getOrDefault(false)
}
