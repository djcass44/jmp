package dev.castive.jmp.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Converts a String to being URL-safe
 */
fun String.safe(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

/**
 * Converts a String to being a UUID
 * @return UUID or null if the String cannot be parsed into a valid UUID
 */
fun String.toUUID(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()