package dev.castive.jmp.util.checks

import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import dev.castive.log2.logw
import okhttp3.OkHttpClient
import okhttp3.Request

class FavCheck: StartupCheck("Image API") {
	override fun runCheck(): Boolean {
		// check if we are actually allowed to make network calls
		if(!EnvUtil.getEnv(EnvUtil.JMP_ALLOW_EGRESS, "true").toBoolean()) {
			"FAV2 access blocked by JMP_ALLOW_EGRESS".logw(javaClass)
			onWarning()
			return true
		}
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