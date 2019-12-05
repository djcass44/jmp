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

import dev.castive.jmp.util.ellipsize
import dev.castive.log2.logd
import dev.castive.log2.logv
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtTokenFilter(private val jwtTokenProvider: JwtTokenProvider): OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain
	) {
		val token = jwtTokenProvider.resolveToken(request)
		if(token != null && jwtTokenProvider.validateToken(token)) {
			val auth = jwtTokenProvider.getAuthentication(token)
			if(auth != null) {
				"Located user principal: ${auth.name} with roles: ${auth.authorities.size}".logv(javaClass)
				SecurityContextHolder.getContext().authentication = auth
			}
			else
				SecurityContextHolder.clearContext()
		}
		else {
			"Failed to parse token: ${token?.ellipsize(24)}".logd(javaClass)
			// ensure context is cleared
			SecurityContextHolder.clearContext()
		}
		// continue with the request
		filterChain.doFilter(request, response)
	}
}
