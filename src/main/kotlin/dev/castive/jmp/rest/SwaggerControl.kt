/*
 *    Copyright 2020 Django Cass
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

package dev.castive.jmp.rest

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class SwaggerControl {

	@Value("\${server.servlet.context-path}")
	private val contextPath = ""

	@GetMapping
	fun index(response: HttpServletResponse) {
		response.sendRedirect("${contextPath}/swagger-ui.html")
	}

	@GetMapping("/csrf")
	fun csrf() {
		// noop to appease swagger
	}

	// not sure where to put this for now
	@GetMapping("/ping")
	fun ping(): String = "pong"
}
