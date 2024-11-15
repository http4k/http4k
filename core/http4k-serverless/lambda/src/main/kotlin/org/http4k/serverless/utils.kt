package org.http4k.serverless

import org.http4k.base64DecodedArray
import org.http4k.core.Body
import org.http4k.core.MemoryBody

internal fun Map<*, *>.getNested(name: String): Map<*, *>? = get(name) as? Map<*, *>
internal fun Map<*, *>.getString(name: String): String? = get(name) as? String
internal fun Map<*, *>.getBoolean(name: String): Boolean? = get(name) as? Boolean

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.getStringMap(name: String): Map<String, String>? = get(name) as? Map<String, String>

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.getStringList(name: String): List<String>? = get(name) as? List<String>

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.multiParameter(name: String) = (getNested(name) as? Map<String, *>)
    ?.flatMap { kv -> (kv.value as? List<String>)?.map { kv.key to it } ?: emptyList() } ?: emptyList()

internal fun Map<String, Any>.toBody() = (getString("body")
    ?.let {
        MemoryBody(
            when {
                getBoolean("isBase64Encoded") == true -> it.base64DecodedArray()
                else -> it.toByteArray()
            }
        )
    }
    ?: Body.EMPTY)

internal fun Map<String, Any>.toHeaders() = (getStringMap("headers")
    ?.map { (k, v) -> v.split(",").map { k to it } }?.flatten()
    ?: emptyList())

internal fun Map<String, Any>.toMultiHeaders() = multiParameter("multiValueHeaders")

internal fun Map<String, Any>.toQueries() = getStringMap("queryStringParameters")?.toList() ?: emptyList()

internal fun Map<String, Any>.toMultiQueries() = multiParameter("multiValueQueryStringParameters")
