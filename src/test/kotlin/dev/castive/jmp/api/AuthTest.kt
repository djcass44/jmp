package dev.castive.jmp.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AuthTest {
	private val dummyText = "This is a test!"

	@Test
	fun testHashMatchValid() {
		val auth = Auth()
		val hash = auth.computeHash(dummyText.toCharArray())
		Assertions.assertTrue(auth.hashMatches(dummyText.toCharArray(), hash))
	}
	@Test
	fun testHashMatchInvalid() {
		val auth = Auth()
		val hash = auth.computeHash(dummyText.toCharArray())
		Assertions.assertFalse(auth.hashMatches("This is a test".toCharArray(), hash))
	}
}