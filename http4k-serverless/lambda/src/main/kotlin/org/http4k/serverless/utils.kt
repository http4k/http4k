package org.http4k.serverless

internal fun Map<*, *>.getNested(name: String): Map<*, *>? = get(name) as? Map<*, *>
internal fun Map<*, *>.getString(name: String): String? = get(name) as? String
internal fun Map<*, *>.getBoolean(name: String): Boolean? = get(name) as? Boolean

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.getStringMap(name: String): Map<String, String>? = get(name) as? Map<String, String>

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.getStringList(name: String): List<String>? = get(name) as? List<String>
