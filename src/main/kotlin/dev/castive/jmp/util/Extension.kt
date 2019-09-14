package dev.castive.jmp.util

import dev.castive.javalin_auth.auth.Roles
import dev.castive.jmp.db.dao.Role
import io.javalin.http.Context
import org.eclipse.jetty.http.HttpStatus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList

/**
 * Converts a String to being URL-safe
 */
fun String.safe(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

/**
 * Converts a String to being a UUID
 * @return UUID or null if the String cannot be parsed into a valid UUID
 */
fun String.toUUID(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

/**
 * Checks whether a String is null or blank that includes ECMAScript nullable types
 */
fun String?.isESNullOrBlank(): Boolean = isNullOrBlank() || this == "null" || this == "undefined"

fun Context.ok(): Context = this.status(HttpStatus.OK_200)

/**
 * Checks whether a DaoRole name is equal to a BasicRole name
 */
fun Role.eq(role: Roles.BasicRoles): Boolean = name == role.name

/**
 * Convert a List to kotlin.collections.ArrayList
 */
fun <T> List<T>.asArrayList(): ArrayList<T> = ArrayList(this)