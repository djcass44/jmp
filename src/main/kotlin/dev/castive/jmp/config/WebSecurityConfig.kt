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

package dev.castive.jmp.config

import dev.castive.jmp.prop.SecurityProps
import dev.castive.jmp.security.JwtTokenFilterConfigurer
import dev.castive.jmp.security.JwtTokenProvider
import dev.castive.log2.loga
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
class WebSecurityConfig @Autowired constructor(
	private val jwtTokenProvider: JwtTokenProvider,
	private val securityProps: SecurityProps
): WebSecurityConfigurerAdapter() {

	override fun configure(http: HttpSecurity) {
		// disable CSRF
		http.csrf().disable()

		// allow cors if enabled
		if(securityProps.allowCors) {
			"Enabling CORS requests for REST resources".loga(javaClass)
			http.cors().configurationSource(corsConfigurationSource())
		}

		// disable sessions
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

		// entrypoints
		http.authorizeRequests()
			.anyRequest().permitAll()

		// apply jwt
		http.apply(JwtTokenFilterConfigurer(jwtTokenProvider))
	}

	override fun configure(web: WebSecurity) {
		// allow swagger access without authentication
		web.ignoring().antMatchers(
			"/v2/api-docs",
			"/swagger-resources/**",
			"/swagger-ui.html",
			"/configuration/**",
			"/webjars/**",
			"/public"
		)
	}

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		return UrlBasedCorsConfigurationSource().apply {
			registerCorsConfiguration("/**", CorsConfiguration().applyPermitDefaultValues().apply {
				addAllowedMethod(HttpMethod.DELETE)
				addAllowedMethod(HttpMethod.PATCH)
				addAllowedMethod(HttpMethod.OPTIONS)
				addAllowedMethod(HttpMethod.PUT)
			})
		}
	}
}
