package dev.castive.jmp.crypto

import dev.castive.log2.logok
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
		"Created new key".logok(javaClass)
	}
}
