package dev.castive.jmp.cache

import java.util.*

interface BaseCacheLayer {
	fun setup()
	fun tearDown()
	fun connected(): Boolean
	fun getUser(id: UUID, token: String): Pair<UUID, String>?
	fun setUser(id: UUID, token: String)
	fun removeUser(token: String)
}