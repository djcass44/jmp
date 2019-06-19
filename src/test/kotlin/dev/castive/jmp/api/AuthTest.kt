package dev.castive.jmp.api

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AuthTest {
	private val dummytText = "This is a test!"

	@Test
	fun testHashMatchValid() {
		val auth = Auth()
		val hash = auth.computeHash(dummytText.toCharArray())
		Assertions.assertTrue(auth.hashMatches(dummytText.toCharArray(), hash))
	}
	@Test
	fun testHashMatchInvalid() {
		val auth = Auth()
		val hash = auth.computeHash(dummytText.toCharArray())
		Assertions.assertFalse(auth.hashMatches("This is a test".toCharArray(), hash))
	}
}