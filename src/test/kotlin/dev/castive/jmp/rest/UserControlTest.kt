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
import dev.castive.jmp.data.BasicAuth
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class UserControlTest: BaseSpringBootTest() {

	@Test
	fun `can create user`() {
		val username = "test"
		val auth = BasicAuth(username, "hunter2")

		Given {
			body(auth)
			header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		} When {
			put("/api/v2/user")
		} Then {
			body(equalTo(username))
			statusCode(HttpStatus.CREATED.value())
		}
	}
}
