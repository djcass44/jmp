package dev.castive.jmp.cache

interface BaseCacheLayer {
	class UserCache(val username: String, val time: Long)

	fun setup(): Boolean
	fun tearDown(): Boolean
	fun connected(): Boolean
	fun getUser(username: String, token: String): Pair<String, String>?
	fun getUser(token: String): String?
	fun setUser(username: String, token: String)
	fun removeUser(token: String)
}