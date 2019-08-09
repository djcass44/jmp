package dev.castive.jmp.util.checks

import com.github.kittinunf.fuel.httpGet
import dev.castive.jmp.util.EnvUtil

class FavCheck: StartupCheck("Image API") {
	override fun runCheck(): Boolean {
		val (_, response, _) = "${EnvUtil.getEnv(EnvUtil.FAV2_URL, "http://localhost:8080")}/healthz".httpGet().responseString()
		if(response.statusCode != 200) {
			onFail()
			return false
		}
		onSuccess()
		return true
	}
}