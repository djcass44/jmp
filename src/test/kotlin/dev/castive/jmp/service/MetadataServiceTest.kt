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

package dev.castive.jmp.service

import com.fasterxml.jackson.databind.ObjectMapper
import dev.castive.jmp.TestUtils.loadFixture
import dev.castive.jmp.component.SocketHandler
import dev.castive.jmp.prop.AppMetadataProps
import dev.castive.jmp.repo.JumpRepo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import kotlin.test.assertNull

class MetadataServiceTest {
	private val jumpRepo = Mockito.mock(JumpRepo::class.java)
	private val metadataProps = Mockito.mock(AppMetadataProps::class.java)
	private val ktorClient = HttpClient(MockEngine) {
		engine {
			addHandler { request ->
				when(request.url.host) {
					"a.com" -> respond(loadFixture("nginx_404.html"))
					"b.com" -> respond(loadFixture("kotlinlang_org.html"))
					"c.com" -> respond(loadFixture("nginx_404_notitle.html"))
					else -> respond(loadFixture("nginx_404.html"))
				}
			}
		}
	}

	private val metadataService = Mockito.spy(MetadataService(jumpRepo, metadataProps, SocketHandler(ObjectMapper())))

	@BeforeEach
	internal fun setUp() {
		Mockito.`when`(metadataService.getClient()).thenReturn(ktorClient)
	}

	@ParameterizedTest
	@ValueSource(strings = [
		"file://google.com",
		"http://google.com",
		"ssh://google.com"
	])
	fun `scheme is always https`(address: String) {
		assertThat(metadataService.getProbableUrl(address), equalTo("https://google.com"))
	}

	@Test
	fun `title can be loaded from simple html`() {
		runBlocking {
			val title = metadataService.getTitle("https://a.com")
			assertThat(title, equalTo("404 Not Found"))
		}
	}

	@Test
	fun `title can be loaded from complex html`() {
		runBlocking {
			val title = metadataService.getTitle("https://b.com")
			assertThat(title, equalTo("Kotlin Programming Language"))
		}
	}

	@Test
	fun `html with no title returns null`() {
		runBlocking {
			val title = metadataService.getTitle("https://c.com")
			assertNull(title)
		}
	}
}
