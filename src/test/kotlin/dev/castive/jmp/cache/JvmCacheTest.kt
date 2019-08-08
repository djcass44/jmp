package dev.castive.jmp.cache

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JvmCacheTest {
	private lateinit var layer: JvmCache

	@BeforeAll
	fun setup() {
		layer = JvmCache()
		assertTrue(layer.connected())
	}

	@Test
	fun testPutUser() {
		val username = "user-${System.currentTimeMillis()}"
		val token = "token-${System.currentTimeMillis()}"

		assertTrue(layer.connected())
		layer.setUser(username, token)

		val result = layer.getUser(token)!!
		assertEquals(username, result.username)
	}
	@Test
	fun testRemoveUser() {
		val username = "user-${System.currentTimeMillis()}"
		val token = "token-${System.currentTimeMillis()}"

		assertTrue(layer.connected())
		layer.setUser(username, token)
		val r = layer.getUser(token)
		assertNotNull(r)
		layer.removeUser(token)

		val result = layer.getUser(token)
		assertNull(result)
	}
	@Test
	fun testPutMisc() {
		val key = "data-key${System.currentTimeMillis()}"
		val value = "data-value${System.currentTimeMillis()}"

		assertTrue(layer.connected())
		layer.set(key, value)

		val result = layer.get(key)
		assertNotNull(result)
		assertEquals(value, result)
	}
}