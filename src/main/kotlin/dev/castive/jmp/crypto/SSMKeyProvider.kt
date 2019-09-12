package dev.castive.jmp.crypto

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest
import dev.castive.eventlog.EventLog
import dev.castive.eventlog.schema.Event
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log

class SSMKeyProvider: KeyProvider() {
	companion object {
		const val shortName = "aws-ssm"
		private val parameterName = EnvUtil.getEnv(EnvUtil.KEY_AWS_SSM_NAME, "JMP_ENCRYPTION_KEY")
	}
	private val client = AWSSimpleSystemsManagementClientBuilder.defaultClient()

	/**
	 * Attempt to load an encryption key from AWS ParameterStore
	 * If there is no key with the name JMP_ENCRYPTION_KEY, jmp will attempt to create one
	 */
	override fun getEncryptionKey(): String {
		val paramRequest = runCatching {
			client.getParameter(GetParameterRequest().withName(parameterName).withWithDecryption(true))
		}
		if(paramRequest.exceptionOrNull() != null) {
			Log.e(javaClass, "GetParameter failed: ${paramRequest.exceptionOrNull()}")
		}
		val param = paramRequest.getOrNull() ?: return createKey()
		if(param.sdkHttpMetadata.httpStatusCode != 200) {
			Log.e(javaClass, "GetParameter returned unexpected: ${param.sdkHttpMetadata.httpStatusCode}")
			return createKey()
		}
		val keyResult = runCatching {
			param.parameter.value
		}
		if(keyResult.exceptionOrNull() != null) {
			Log.e(javaClass, "Failed to get parameter: ${keyResult.exceptionOrNull()}")
			return createKey()
		}
		super.postCreate()
		Log.ok(javaClass, "We appear to have successfully retrieved the encryption key")
		return keyResult.getOrNull() ?: createKey()
	}

	/**
	 * Generate a key and attempt to store it in AWS ParameterStore
	 */
	override fun createKey(): String {
		EventLog.post(Event(type = "CREATE", resource = javaClass, causedBy = javaClass))
		Log.v(javaClass, "Creating new encryption key...")
		val key = super.createKey()
		val r = client.putParameter(PutParameterRequest().withName(parameterName).withType(ParameterType.SecureString).withValue(key))
		if(r.sdkHttpMetadata.httpStatusCode != 200) {
			Log.f(javaClass, "Failed to write encryption key, this will cause issues!")
		}
		return key
	}
}