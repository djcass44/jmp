/*
 *    Copyright [2019 Django Cass
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

package dev.castive.jmp.auth

import dev.dcas.util.extend.json
import dev.dcas.util.extend.parse
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigBuilderTest {
	@Test
	fun `test getting invalid jmp dot json`() {
		val builder = Mockito.mock(ConfigBuilder::class.java)
		// return null when getting the file
		Mockito.`when`(builder.getDataFile()).thenReturn(null)

		assertEquals(builder.getDefault(), builder.get())
	}
	@Test
	fun `test getting valid jmp dot json`() {
		val builder = Mockito.mock(ConfigBuilder::class.java)
		// return null when getting the file
		Mockito.`when`(builder.getDataFile()).thenReturn(File("/tmp/test-jmp.json"))

		assertEquals(builder.getDefault(), builder.get())
	}
	@Test
	fun `test config version fails when incorrect`() {
		val config = """
			{
				"version": ""
			}
		""".parse(ConfigBuilder.JMPConfiguration::class.java)
		println(config.json())
		assertFalse(ConfigBuilder().validateConfig(config))
	}
	@Test
	fun `test config version passes when correct`() {
		val config = """
			{
				"version": "2019-10-02"
			}
		""".parse(ConfigBuilder.JMPConfiguration::class.java)
		println(config.json())
		assertTrue(ConfigBuilder().validateConfig(config))
	}
}
