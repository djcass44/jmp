package dev.castive.jmp.util.checks

import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import okhttp3.OkHttpClient
import okhttp3.Request

class FavCheck: StartupCheck("Image API") {
	override fun runCheck(): Boolean {
		val request = Request.Builder().url("${EnvUtil.getEnv(EnvUtil.FAV2_URL, "http://localhost:8080")}/healthz").get().build()
		return try {
			OkHttpClient().newCall(request).execute().use {
				if(it.code != 200) {
					onFail()
					return false
				}
				it.close()
				onSuccess()
				return true
			}
		}
		catch (e: Exception) {
			Log.e(javaClass, "Failed to reach fav2: $e")
			onFail()
			false
		}
	}
}