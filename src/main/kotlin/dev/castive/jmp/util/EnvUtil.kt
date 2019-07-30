package dev.castive.jmp.util

object EnvUtil {
	fun getEnv(name: String, default: String = ""): String {
		val env = System.getenv(name)
		return if (env.isNullOrEmpty()) default else env
	}

	const val KEY_REALM = "JMP_KEY_REALM"
	const val CASE_SENSITIVE = "JMP_CASE_SENSITIVE"
	const val PORT = "PORT"

	const val SOCKET_ENABLED = "SOCKET_ENABLED"
	const val SOCKET_PORT = "SOCKET_PORT"
	const val SOCKET_HOST = "SOCKET_HOST"

	const val LOG_ENABLED = "LOG_ENABLED"
	const val LOG_LOCATION = "LOG_DIRECTORY"

	const val GITHUB_ENABLED = "GITHUB_ENABLED"
	const val GOOGLE_ENABLED = "GOOGLE_ENABLED"
}