package dev.castive.jmp.cache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap
import dev.castive.jmp.db.Util
import dev.castive.jmp.util.SystemUtil
import dev.castive.log2.Log
import java.util.*

class HazelcastCacheLayer: BaseCacheLayer {
	lateinit var hzInstance: HazelcastInstance
	private lateinit var userMap: IMap<String, String>
	private lateinit var miscMap: IMap<String, String>

	override fun setup(): Boolean {
		if(connected()) {
			Log.e(javaClass, "We already have an active Hazelcast instance")
			return false
		}
		Log.i(javaClass, "Hazelcast is starting...")
		hzInstance = Hazelcast.getOrCreateHazelcastInstance(Config("jmp-hz"))
		userMap = hzInstance.getMap("claimedUsers")
		miscMap = hzInstance.getMap("misc")
		Log.v(javaClass, "Hazelcast contains map with ${userMap.size} entries")
		return true
	}

	override fun tearDown(): Boolean {
		if(!connected()) {
			Log.e(javaClass, "There is no connected instance to shutdown")
			return false
		}
		Log.i(javaClass, "Hazelcast is being shut down...")
		hzInstance.lifecycleService.shutdown()
		return true
	}

	override fun connected(): Boolean {
		if(!this::hzInstance.isInitialized) {
			Log.e(javaClass, "Hazelcast is not initialised. Please run $javaClass::setup() first")
			return false
		}
		return hzInstance.lifecycleService.isRunning
	}

	override fun getUser(id: UUID, token: String): Pair<UUID, String>? {
		if(userMap.isEmpty) return null
		val guess = get(userMap[token] ?: return null)
		if(System.currentTimeMillis() - guess.time > 5000) return null
		if(guess.id == id) return Pair(guess.id, token)
		return null
	}

	override fun getUser(token: String): UUID? {
		if(userMap.isEmpty) return null
		val id = userMap[token] ?: return null
		return Util.getSafeUUID(id)
	}

	override fun setUser(id: UUID, token: String) {
		userMap[token] = set(BaseCacheLayer.UserCache(id, System.currentTimeMillis()))
	}

	override fun removeUser(token: String) {
		userMap.remove(token)
	}

	fun getMisc(key: String): String? {
		return miscMap[key]
	}
	fun setMisc(key: String, value: String) {
		miscMap[key] = value
	}

	private fun get(str: String): BaseCacheLayer.UserCache = SystemUtil.gson.fromJson(str, BaseCacheLayer.UserCache::class.java)
	private fun set(uc: BaseCacheLayer.UserCache) = SystemUtil.gson.toJson(uc)
}