package dev.castive.jmp

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArgumentsTest {
	@Test
	fun testCors() {
		val args = arrayOf("--enable-cors")
		val arguments = Arguments(args)
		assertTrue(arguments.enableCors)
		assertFalse(arguments.enableDev)
	}
	@Test
	fun testDev() {
		val args = arrayOf("--enable-dev")
		val arguments = Arguments(args)
		assertTrue(arguments.enableCors)
		assertTrue(arguments.enableDev)
	}
}