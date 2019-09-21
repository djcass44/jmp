package dev.castive.jmp.util

object EnvUtil {
	fun getEnv(name: String, default: String = ""): String {
		val env = System.getenv(name)
		return if (env.isNullOrEmpty()) default else env
	}

	const val JMP_INDEX_PATH = "JMP_INDEX_PATH"

	// allow exception information in the tracker (default false)
	const val JMP_ALLOW_ERROR_INFO = "JMP_ALLOW_ERROR_INFO"
	// set the source of JWT encryption key
	const val KEY_REALM = "JMP_KEY_REALM"
	const val KEY_AWS_SSM_NAME = "JMP_KEY_AWS_SSM_NAME"
	// whether jumps should be case sensitive
	const val CASE_SENSITIVE = "JMP_CASE_SENSITIVE"
	// http port
	const val PORT = "PORT"

	// whether logs should be written to a file
	const val LOG_ENABLED = "LOG_ENABLED"
	// the directory to write logs (if enabled)
	const val LOG_LOCATION = "LOG_DIRECTORY"

	// the url of the fav2 server
	const val FAV2_URL = "FAV2_URL"

	// whether github oauth2 is enabled
	const val GITHUB_ENABLED = "GITHUB_ENABLED"
	// whether google oauth2 is enabled
	const val GOOGLE_ENABLED = "GOOGLE_ENABLED"
}