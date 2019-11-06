package dev.castive.jmp.crypto

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.*
import dev.castive.eventlog.EventLog
import dev.castive.eventlog.schema.Event
import dev.castive.eventlog.schema.EventType
import dev.castive.jmp.util.EnvUtil
import dev.castive.log2.Log
import dev.castive.log2.loge

class SSMKeyProvider(
	private val client: AWSSimpleSystemsManagement = AWSSimpleSystemsManagementClientBuilder.defaultClient()
): KeyProvider() {
	companion object {
		const val shortName = "aws-ssm"
		private val parameterName = EnvUtil.getEnv(EnvUtil.KEY_AWS_SSM_NAME, "JMP_ENCRYPTION_KEY")
	}

	/**
	 * Attempt to load an encryption key from AWS ParameterStore
	 * If there is no key with the name JMP_ENCRYPTION_KEY, jmp will attempt to create one
	 */
	override fun getEncryptionKey(): String {
		val paramRequest = runCatching {
			client.getParameter(GetParameterRequest().withName(parameterName).withWithDecryption(true))
		}
		paramRequest.exceptionOrNull()?.let {
			"GetParameter failed: ${paramRequest.exceptionOrNull()}".loge(javaClass)
		}
		val param = paramRequest.getOrNull()
		val keyResult = validateParameter(param) ?: createKey()
		EventLog.post(Event(type = EventType.READ, resource = Parameter::class.java, causedBy = javaClass))
		super.postCreate()
		Log.ok(javaClass, "We appear to have successfully retrieved the encryption key")
		return keyResult
	}

	internal fun validateParameter(param: GetParameterResult?): String? {
		if(param == null) return null
		if(param.sdkHttpMetadata.httpStatusCode != 200) {
			Log.e(javaClass, "GetParameter returned unexpected: ${param.sdkHttpMetadata.httpStatusCode}")
			return null
		}
		val keyResult = runCatching {
			param.parameter.value
		}
		keyResult.exceptionOrNull()?.let {
			Log.e(javaClass, "Failed to get parameter: ${keyResult.exceptionOrNull()}")
			return null
		}
		return keyResult.getOrNull()
	}

	/**
	 * Generate a key and attempt to store it in AWS ParameterStore
	 */
	override fun createKey(): String {
		EventLog.post(Event(type = EventType.CREATE, resource = javaClass, causedBy = javaClass))
		Log.v(javaClass, "Creating new encryption key...")
		val key = super.createKey()
		val r = client.putParameter(PutParameterRequest().withName(parameterName).withType(ParameterType.SecureString).withValue(key))
		if(r.sdkHttpMetadata.httpStatusCode != 200) {
			Log.f(javaClass, "Failed to write encryption key, this will cause issues!")
		}
		return key
	}
}