package dev.castive.jmp.cache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap
import dev.castive.log2.Log
import java.util.*

class HazelcastCacheLayer: BaseCacheLayer {
	private lateinit var hzInstance: HazelcastInstance
	private lateinit var map: IMap<String, UUID>

	override fun setup() {
		hzInstance = Hazelcast.getOrCreateHazelcastInstance(Config("jmp-hz"))
		map = hzInstance.getMap("claimedUsers")
		Log.v(javaClass, "Hazelcast contains map with ${map.size} entries")
	}

	override fun tearDown() {
		hzInstance.shutdown()
	}

	override fun connected(): Boolean {
		if(!this::hzInstance.isInitialized) {
			Log.e(javaClass, "Hazelcast is not initialised. Please run $javaClass::setup() first")
			return false
		}
		// TODO add actual method of checking for connection
		return true
	}

	override fun getUser(id: UUID, token: String): Pair<UUID, String>? {
		if(map.isEmpty) return null
		val guess = map[token]
		if(guess == id) return Pair(guess, token)
		return null
	}

	override fun setUser(id: UUID, token: String) {
		map[token] = id
	}

	override fun removeUser(token: String) {
		map.remove(token)
	}
}