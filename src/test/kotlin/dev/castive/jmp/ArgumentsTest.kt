package dev.castive.jmp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ArgumentsTest {
	@Test
	fun testCors() {
		val args = arrayOf("using", "env", "--enable-cors")
		val arguments = Arguments(args)
		assertTrue(arguments.enableCors)
		assertFalse(arguments.enableDev)
	}
	@Test
	fun testDev() {
		val args = arrayOf("using", "env", "--enable-dev")
		val arguments = Arguments(args)
		assertTrue(arguments.enableCors)
		assertTrue(arguments.enableDev)
	}
	@ParameterizedTest
	@ValueSource(ints = [0, 1, 2, 3, 4, 5, 6])
	fun testDebugLevel(level: Int) {
		val args = arrayOf("using", "env", "-d", level.toString())
		val arguments = Arguments(args)
		assertEquals(arguments.debugLevel, level)
	}
	@ParameterizedTest
	@ValueSource(strings = ["test", "", "2"])
	fun testInvalidDebugLevel(level: String) {
		val args = arrayOf("using", "env", "-d", level)
		val arguments = Arguments(args)
		assertEquals(2, arguments.debugLevel)
	}
	@Test
	fun testMissingDebugLevel() {
		val args = arrayOf("using", "env")
		val arguments = Arguments(args)
		assertEquals(2, arguments.debugLevel)
	}
}