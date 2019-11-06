package dev.castive.jmp.cache

import java.util.concurrent.ConcurrentHashMap

class JvmCache: BaseCacheLayer {
	private val userMap = ConcurrentHashMap<String, BaseCacheLayer.UserCache>()
	private val miscMap = ConcurrentHashMap<String, String>()

	override fun setup() = true

	override fun tearDown() = true

	override fun connected() = true

	override fun getUser(token: String): BaseCacheLayer.UserCache? {
		if(userMap.isEmpty()) return null
		val res = userMap[token] ?: return null
		// Consider cache of over 10 seconds to be stale
		if(System.currentTimeMillis() - res.time > 10_000) return null
		return res
	}

	override fun setUser(username: String, token: String) {
		userMap[token] = BaseCacheLayer.UserCache(username, System.currentTimeMillis())
	}

	override fun removeUser(token: String) {
		userMap.remove(token)
	}

	override fun get(key: String): String? {
		return miscMap[key]
	}
	override fun set(key: String, value: String) {
		miscMap[key] = value
	}

	override fun size(): Int {
		return userMap.size
	}
}