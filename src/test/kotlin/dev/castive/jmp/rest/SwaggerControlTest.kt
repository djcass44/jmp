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

import dev.castive.jmp.BaseSpringBootTest
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class SwaggerControlTest: BaseSpringBootTest() {

	@Test
	fun `csrf no-op returns nothing`() {
		Given {
			header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		} When {
			get("/api/csrf")
		} Then {
			body(CoreMatchers.equalTo(""))
			statusCode(HttpStatus.OK.value())
		}
	}

	@Test
	fun `ping returns pong`() {
		Given {
			header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		} When {
			get("/api/ping")
		} Then {
			body(CoreMatchers.equalTo("pong"))
			statusCode(HttpStatus.OK.value())
		}
	}
}
