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

import dev.castive.jmp.service.auth.OAuth2Service
import dev.castive.jmp.util.ellipsize
import dev.castive.log2.logd
import dev.castive.log2.logi
import dev.castive.log2.logv
import dev.castive.log2.logw
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class OAuth2TokenFilter(
	private val oauth2Service: OAuth2Service
): OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain
	) {
		val source: String? = request.getHeader(SecurityConstants.sourceHeader)
		if(source != null && !source.startsWith(SecurityConstants.sourceOAuth2)) {
			// this is not for us to mess with
			filterChain.doFilter(request, response)
			return
		}
		if(source == null) {
			"Unable to parse OAuth2 context without a valid source identifier".logw(javaClass)
			SecurityContextHolder.clearContext()
			filterChain.doFilter(request, response)
			return
		}
		val providerName = source.split("/")[1]
		val provider = oauth2Service.findProviderByName(providerName) ?: run {
			"Unable to find wired provider with name: $providerName".logi(javaClass)
			SecurityContextHolder.clearContext()
			filterChain.doFilter(request, response)
			return
		}
		val token = resolveToken(request)
		if(token != null && provider.isTokenValid(token)) {
			val auth = oauth2Service.getAuthentication(token)
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

	/**
	 * Extract an OAuth2 token from a request header
	 */
	private fun resolveToken(request: HttpServletRequest): String? {
		val source = request.getHeader(SecurityConstants.sourceHeader)
		// oauth2 must advertise source as oauth2/name (e.g. oauth2/github, oauth/google)
		if(!source.startsWith("${SecurityConstants.sourceOAuth2}/"))
			return null
		val token = request.getHeader(SecurityConstants.authHeader) ?: return null
		// token MUST start with 'Bearer '
		if(token.startsWith("Bearer "))
			return token.substring(7)
		return null
	}
}
