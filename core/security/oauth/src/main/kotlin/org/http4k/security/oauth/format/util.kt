package org.http4k.security.oauth.format

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token.NULL

internal fun <T> Map<String, Any>.value(name: String, fn: Function1<String, T>) =
    this[name]?.toString()?.let(fn)

internal fun Map<*, *>.string(name: String) = this[name]?.toString()
internal fun Map<*, *>.boolean(name: String) = this[name]?.toString()?.toBoolean()
internal fun Map<*, *>.long(name: String) = this[name]?.toString()?.toBigDecimal()?.toLong()

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.map(name: String) = this[name] as Map<String, Any>?

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.strings(name: String) = this[name] as List<String>?


fun JsonReader.readStringArray(): Array<String> {
    val result = mutableListOf<String>()
    beginArray()
    while (hasNext()) {
        result.add(nextString())
    }
    endArray()
    return result.toTypedArray()
}

fun JsonReader.nextStringOrNull(): String? =
    when (NULL) {
        peek() -> {
            nextNull<String>()
            null
        }

        else -> {
            nextString()
        }
    }
