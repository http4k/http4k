package org.http4k.security.oauth.server

internal fun <T> Map<String, Any>.value(name: String, fn: Function1<String, T>) =
    this[name]?.toString()?.let(fn)

internal fun Map<*, *>.string(name: String) = this[name]?.toString()
internal fun Map<*, *>.boolean(name: String) = this[name]?.toString()?.toBoolean()
internal fun Map<*, *>.long(name: String) = this[name]?.toString()?.toBigDecimal()?.toLong()

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.map(name: String) = this[name] as Map<String, Any>?

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.strings(name: String) = this[name] as List<String>?

