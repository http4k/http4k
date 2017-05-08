package org.http4k

import java.nio.ByteBuffer
import java.util.*

fun ByteBuffer.asString(): String = String(array())

fun String.asByteBuffer(): ByteBuffer = ByteBuffer.wrap(this.toByteArray())

fun String.quoted() = "\"${this.replace("\"", "\\\"")}\""

fun String.unquoted(): String = replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "").replace("\\\"", "\"")

fun StringBuilder.appendIfNotBlank(valueToCheck: String, vararg toAppend: String): StringBuilder =
    appendIf({ valueToCheck.isNotBlank() }, *toAppend)

fun StringBuilder.appendIfNotEmpty(valueToCheck: List<Any>, vararg toAppend: String): StringBuilder =
    appendIf({ valueToCheck.isNotEmpty() }, *toAppend)

fun StringBuilder.appendIfPresent(valueToCheck: Any?, vararg toAppend: String): StringBuilder =
    appendIf({ valueToCheck != null }, *toAppend)

fun StringBuilder.appendIf(condition: () -> Boolean, vararg toAppend: String): StringBuilder {
    if (condition()) toAppend.forEach { append(it) }
    return this
}

fun String.base64Decoded(): String = String(Base64.getDecoder().decode(this))

fun String.base64Encode() = String(Base64.getEncoder().encode(toByteArray()))