package dev.castive.jmp.cache

interface BaseCacheLayer {
	class UserCache(val username: String, val time: Long)

	fun setup(): Boolean
	fun tearDown(): Boolean
	fun connected(): Boolean
	fun getUser(token: String): UserCache?
	fun setUser(username: String, token: String)
	fun removeUser(token: String)
	fun size(): Int
	fun get(key: String): String?
	fun set(key: String, value: String)
}