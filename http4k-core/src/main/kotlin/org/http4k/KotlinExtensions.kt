package org.http4k

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.Base64

fun ByteBuffer.length() = limit() - position()

fun ByteBuffer.asString(): String = String(array(), position(), length())

fun ByteBuffer.base64Encode() : String = Base64.getEncoder().encodeToString(array())

fun String.asByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray())

fun String.quoted() = "\"${replace("\"", "\\\"")}\""

fun String.unquoted(): String = replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "").replace("\\\"", "\"")

fun StringBuilder.appendIfNotBlank(valueToCheck: String, vararg toAppend: String): StringBuilder =
    appendIf({ valueToCheck.isNotBlank() }, *toAppend)

fun StringBuilder.appendIfNotEmpty(valueToCheck: List<Any>, vararg toAppend: String): StringBuilder =
    appendIf({ valueToCheck.isNotEmpty() }, *toAppend)

fun StringBuilder.appendIfPresent(valueToCheck: Any?, vararg toAppend: String): StringBuilder =
    appendIf({ valueToCheck != null }, *toAppend)

fun StringBuilder.appendIf(condition: () -> Boolean, vararg toAppend: String): StringBuilder = apply {
    if (condition()) toAppend.forEach { append(it) }
}

fun String.base64Decoded(): String = String(Base64.getDecoder().decode(this))

fun String.base64Encode() = String(Base64.getEncoder().encode(toByteArray()))

fun String.urlEncoded(): String = URLEncoder.encode(this, "utf-8")
