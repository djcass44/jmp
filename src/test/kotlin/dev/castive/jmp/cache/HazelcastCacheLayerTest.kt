package dev.castive.jmp.cache

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

class HazelcastCacheLayerTest {
	@Test
	fun testLifecycle() {
		val layer = HazelcastCacheLayer()
		assertFalse(layer.connected())
		layer.setup()
		assertTrue(layer.connected())
		layer.tearDown()
		assertFalse(layer.connected())
	}
	@Test
	fun testDupeBlocking() {
		val layer = HazelcastCacheLayer()
		assertFalse(layer.connected())
		layer.setup()
		assertTrue(layer.connected())
		assertFalse(layer.setup())
		layer.tearDown()
		assertFalse(layer.connected())
		assertFalse(layer.tearDown())
	}
}
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HazelcastCacheLayerActiveTest {
	private lateinit var layer: HazelcastCacheLayer

	@BeforeAll
	fun setup() {
		layer = HazelcastCacheLayer()
		assertFalse(layer.connected())
		layer.setup()
		assertTrue(layer.connected())
	}
	@AfterAll
	fun tearDown() {
		assertTrue(layer.connected())
		layer.tearDown()
		assertFalse(layer.connected())
	}

	@Test
	fun testPutUser() {
		val username = "user-${System.currentTimeMillis()}"
		val token = "token-${System.currentTimeMillis()}"

		assertTrue(layer.connected())
		layer.setUser(username, token)

		val result = layer.get(layer.getUser(token)!!)
		assertNotNull(result)
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
		layer.setMisc(key, value)

		val result = layer.getMisc(key)
		assertNotNull(result)
		assertEquals(value, result)
	}
}