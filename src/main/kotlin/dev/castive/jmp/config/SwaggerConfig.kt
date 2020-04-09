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

import dev.dcas.jmp.spring.security.SecurityConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.*
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class SwaggerConfig: WebMvcConfigurationSupport() {

	@Bean
	fun getSwaggerConfig(): Docket = Docket(DocumentationType.SWAGGER_2).select()
		.apis(RequestHandlerSelectors.basePackage("dev"))
		.paths(PathSelectors.any())
		.build()
		.securityContexts(listOf(securityContext()))
		.securitySchemes(listOf(apiKey()))
		.apiInfo(apiInfo())

	private fun apiInfo(): ApiInfo = ApiInfoBuilder()
		.title("JMP")
		.description("Utility for quickly navigating to websites & addresses")
		.version("0.5")
		.contact(Contact("Django Cass", "https://dcas.dev", "django@dcas.dev"))
		.license("Apache License Version 2.0")
		.licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
		.build()

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry.addResourceHandler("swagger-ui.html")
			.addResourceLocations("classpath:/META-INF/resources/")
		registry.addResourceHandler("/webjars/**")
			.addResourceLocations("classpath:/META-INF/resources/webjars/")
	}

	private fun apiKey(): ApiKey = ApiKey(SecurityConstants.authHeader, SecurityConstants.authHeader, "header")

	/**
	 * Default configuration required to fix https://github.com/springfox/springfox/issues/2194
	 */
	private fun securityContext(): SecurityContext = SecurityContext.builder()
		.securityReferences(defaultAuth())
		.forPaths(PathSelectors.any())
		.build()

	/**
	 * Default configuration required to fix https://github.com/springfox/springfox/issues/2194
	 */
	private fun defaultAuth(): List<SecurityReference> {
		val scopes = arrayOf(
			AuthorizationScope("global", "accessEverything")
		)
		return listOf(SecurityReference(SecurityConstants.authHeader, scopes))
	}
}
