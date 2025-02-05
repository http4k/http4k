package org.http4k.mcp.client.internal

import org.http4k.format.MoshiObject
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

internal inline fun <reified T : Any> McpNodeType.asAOrThrow() = with(McpJson) {
    val obj = this@asAOrThrow as MoshiObject
    val error = obj["error"]
    when {
        error != null -> error("Failed: " + asFormatString(error))
        else -> asA<T>(compact(obj["result"] ?: nullNode()))
    }
}

