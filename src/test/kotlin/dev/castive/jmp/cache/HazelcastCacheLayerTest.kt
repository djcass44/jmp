package dev.castive.jmp.cache

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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