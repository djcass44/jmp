package dev.castive.jmp.crypto

import dev.castive.eventlog.EventLog
import dev.castive.eventlog.schema.Event
import dev.castive.eventlog.schema.EventType
import java.util.*
import javax.crypto.KeyGenerator

open class KeyProvider {
	companion object {
		const val shortName = "java"
	}

	private val generator = KeyGenerator.getInstance("AES").apply {
		init(256)
	}

	open fun getEncryptionKey(): String {
		postCreate()
		return createKey()
	}

	/**
	 * Generate a basic key
	 */
	open fun createKey(): String {
		return Base64.getUrlEncoder().encodeToString(generator.generateKey().encoded)
	}

	fun postCreate() {
		EventLog.post(Event(type = EventType.CREATE, resource = javaClass, causedBy = javaClass))
	}
}