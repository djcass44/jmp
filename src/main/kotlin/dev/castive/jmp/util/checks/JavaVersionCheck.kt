package dev.castive.jmp.util.checks

import dev.castive.log2.Log

class JavaVersionCheck: StartupCheck("Java version") {
	override fun runCheck(): Boolean {
		val version = getVersion(System.getProperty("java.specification.version"))
		return when {
			version == null -> {
				Log.w(javaClass, "Unable to get Java version")
				onWarning()
				false
			}
			version < 11 -> {
				onFail()
				false
			}
			else -> {
				onSuccess()
				true
			}
		}
	}
	internal fun getVersion(data: String): Int? {
		return if(data.contains(".")) {
			data.split(".")[1].toIntOrNull()
		}
		else
			data.toIntOrNull()
	}
}