package dev.castive.jmp.cache

import java.util.*

interface BaseCacheLayer {
	class UserCache(val id: UUID, val time: Long)

	fun setup(): Boolean
	fun tearDown(): Boolean
	fun connected(): Boolean
	fun getUser(id: UUID, token: String): Pair<UUID, String>?
	fun getUser(token: String): UUID?
	fun setUser(id: UUID, token: String)
	fun removeUser(token: String)
}