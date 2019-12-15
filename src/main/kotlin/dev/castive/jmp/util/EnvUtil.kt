package dev.castive.jmp.util

@Deprecated("Replaced by Spring environment")
object EnvUtil {
	const val JMP_PROXY_URL = "JMP_PROXY_URL"
	const val JMP_HOME = "JMP_HOME"

	/**
	 * set the source of JWT encryption key
	 */
	const val KEY_REALM = "JMP_KEY_REALM"
	const val KEY_AWS_SSM_NAME = "JMP_KEY_AWS_SSM_NAME"

}
