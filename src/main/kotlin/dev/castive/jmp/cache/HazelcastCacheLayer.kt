package dev.castive.jmp.cache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap
import dev.castive.jmp.util.SystemUtil
import dev.castive.log2.Log

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

	override fun getUser(username: String, token: String): Pair<String, String>? {
		if(userMap.isEmpty) return null
		val guess = get(userMap[token] ?: return null)
		if(System.currentTimeMillis() - guess.time > 5000) return null
		if(guess.username == username) return Pair(guess.username, token)
		return null
	}

	override fun getUser(token: String): String? {
		if(userMap.isEmpty) return null
		return userMap[token] ?: return null
	}

	override fun setUser(username: String, token: String) {
		userMap[token] = set(BaseCacheLayer.UserCache(username, System.currentTimeMillis()))
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

	fun get(str: String): BaseCacheLayer.UserCache = SystemUtil.gson.fromJson(str, BaseCacheLayer.UserCache::class.java)
	private fun set(uc: BaseCacheLayer.UserCache) = SystemUtil.gson.toJson(uc)
}