package org.http4k.mcp.client.internal

import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage.Event

internal inline fun <reified T : Any> Event.asAOrThrow() = with(McpJson) {
    val result = JsonRpcResult(this, (parse(data) as MoshiObject).attributes)
    when {
        result.isError() -> error("Failed: " + asFormatString(result.error ?: nullNode()))
        else -> asA<T>(compact(result.result ?: nullNode()))
    }
}

