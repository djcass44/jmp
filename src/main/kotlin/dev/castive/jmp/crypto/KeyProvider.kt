package dev.castive.jmp.crypto

import dev.castive.eventlog.EventLog
import dev.castive.eventlog.schema.Event
import dev.castive.eventlog.schema.EventType
import org.apache.commons.codec.binary.Base64
import javax.crypto.KeyGenerator

open class KeyProvider {
	companion object {
		const val shortName = "java"
	}
	private val generator = KeyGenerator.getInstance("AES")

	init {
		generator.init(256)
	}

	open fun getEncryptionKey(): String {
		postCreate()
		return createKey()
	}

	/**
	 * Generate a basic key
	 */
	open fun createKey(): String {
		return Base64.encodeBase64String(generator.generateKey().encoded)
	}

	fun postCreate() {
		EventLog.post(Event(type = EventType.CREATE, resource = javaClass, causedBy = javaClass))
	}
}