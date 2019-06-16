package dev.castive.jmp.db

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.*

class UtilTest {
	@Test
	fun testValidUUID() {
		val uuid = UUID.randomUUID().toString()
		assertNotNull(Util.getSafeUUID(uuid))
	}
	@Test
	fun testInvalidUUID() {
		val uuid = "This is definitely not a UUID"
		assertNull(Util.getSafeUUID(uuid))
	}
}