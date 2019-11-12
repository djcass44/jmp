package dev.castive.jmp.util

object EnvUtil {
	/**
	 * Load an environment variable from the system, or return a default value if it can't be found
	 */
	@Deprecated(message = "Replaced by castive-utilities", level = DeprecationLevel.WARNING)
	fun getEnv(name: String, default: String = ""): String {
		val env = System.getenv(name)
		return if (env.isNullOrEmpty()) default else env
	}

	const val DRIVER_URL = "DRIVER_URL"
	const val DRIVER_CLASS = "DRIVER_CLASS"
	const val DRIVER_USER = "DRIVER_USER"
	const val DRIVER_PASSWORD = "DRIVER_PASSWORD"

	const val JMP_PROXY_URL = "JMP_PROXY_URL"
	const val JMP_HTTP_SECURE = "JMP_HTTP_SECURE"
	const val JMP_SSL_KEYSTORE = "JMP_SSL_KEYSTORE"
	const val JMP_SSL_PASSWORD = "JMP_SSL_PASSWORD"
	/**
	 * Use HTTP2 (requires SSL setup)
	 */
	const val JMP_HTTP2 = "JMP_HTTP2"

	const val JMP_HOME = "JMP_HOME"

	/**
	 * allow jmp to make outbound network requests (e.g. for scraping website metadata) (default true)
	 * note: this doesn't effect requests made to authentication servers (e.g. crowd, oauth2)
	 */
	const val JMP_ALLOW_EGRESS = "JMP_ALLOW_EGRESS"

	/**
	 *  allow exception information in the tracker (default false)
	 */
	const val JMP_ALLOW_ERROR_INFO = "JMP_ALLOW_ERROR_INFO"
	/**
	 * set the source of JWT encryption key
	 */
	const val KEY_REALM = "JMP_KEY_REALM"
	const val KEY_AWS_SSM_NAME = "JMP_KEY_AWS_SSM_NAME"
	/**
	 * whether jumps should be case sensitive
	 */
	const val CASE_SENSITIVE = "JMP_CASE_SENSITIVE"
	/**
	 * http port
	 */
	const val PORT = "PORT"

	/**
	 * whether logs should be written to a file
	 */
	const val LOG_ENABLED = "LOG_ENABLED"

	/**
	 * the url of the fav2 server
	 */
	const val FAV2_URL = "FAV2_URL"
}
