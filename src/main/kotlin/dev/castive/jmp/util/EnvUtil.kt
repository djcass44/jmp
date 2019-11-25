package dev.castive.jmp.util

object EnvUtil {
	const val JMP_PROXY_URL = "JMP_PROXY_URL"
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
	 * the url of the fav2 server
	 */
	const val FAV2_URL = "FAV2_URL"
}
