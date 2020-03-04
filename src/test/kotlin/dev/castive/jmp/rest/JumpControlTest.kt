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

import dev.dcas.util.spring.responses.BadRequestResponse
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito

class JumpControlTest {

	private val jump = Mockito.mock(JumpControl::class.java)

	/**
	 * Setup mocks
	 */
	@BeforeEach
	internal fun setUp() {
		Mockito.`when`(jump.getValidTarget(Mockito.anyString(), Mockito.any())).thenCallRealMethod()
	}

	@ParameterizedTest(name = "{0} should be decoded to {1}")
	@CsvSource("dGVzdA==,test", "am1w,jmp")
	fun `valid targets are returned correctly`(given: String, expected: String) {
		assertThat(jump.getValidTarget(given), CoreMatchers.equalTo(expected))
	}

	/**
	 * Text that is encoded with varying degrees of whitespace padding should be decoded to the same value
	 */
	@ParameterizedTest
	@CsvSource("dGVzdA==,ICAgICB0ZXN0ICAgIA==,test", "am1w,IGptcA==,jmp")
	fun `targets should be trimmed`(given: String, padded: String, expected: String) {
		val t1 = jump.getValidTarget(given)
		val t2 = jump.getValidTarget(padded)
		assertThat(t1, CoreMatchers.equalTo(t2))
		assertThat(t1, CoreMatchers.equalTo(expected))
	}

	@Test
	fun `empty targets should throw if no id is given`() {
		assertThrows<BadRequestResponse> {
			jump.getValidTarget("", null)
		}
	}

	@ParameterizedTest
	@ValueSource(strings = [" ", "aGV%sb-G8sIHdvcmxkIQ==", "====="])
	fun `non-base64 data should throw if no id is given`(value: String) {
		assertThrows<BadRequestResponse> {
			jump.getValidTarget(value, null)
		}
	}
}
