package dev.castive.jmp.util.checks

import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import dev.castive.log2.logw
import dev.dcas.util.extend.env
import okhttp3.OkHttpClient
import okhttp3.Request

class FavCheck: StartupCheck("Image API") {
	override fun runCheck(): Boolean {
		// check if we are actually allowed to make network calls
		if(!EnvUtil.JMP_ALLOW_EGRESS.env("true").toBoolean()) {
			"FAV2 access blocked by egress policy".logw(javaClass)
			onWarning()
			return true
		}
		val request = Request.Builder().url("${EnvUtil.FAV2_URL.env("http://localhost:8080")}/actuator/health").get().build()
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
