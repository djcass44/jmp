package dev.castive.jmp

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArgumentsTest {
	@Test
	fun `enabling cors sets correct flags`() {
		val args = arrayOf("--enable-cors")
		val arguments = Arguments(args)
		assertTrue(arguments.enableCors)
		assertFalse(arguments.enableDev)
	}
	@Test
	fun `enabling dev sets correct flags`() {
		val args = arrayOf("--enable-dev")
		val arguments = Arguments(args)
		assertTrue(arguments.enableCors)
		assertTrue(arguments.enableDev)
	}
}
