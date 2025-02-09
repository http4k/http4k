package org.http4k.mcp.client.internal

import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

internal inline fun <reified T : Any> McpNodeType.asAOrThrow() = with(McpJson) {
    val obj = this@asAOrThrow as MoshiObject
    val error = obj["error"]
    when {
        error != null -> throw McpException(
            when (error) {
                is MoshiObject -> ErrorMessage(
                    error["code"]?.let { McpJson.integer(it).toInt() } ?: -1,
                    error["message"]?.let { McpJson.text(it) } ?: "Unknown error"
                )

                else -> ErrorMessage(-1, error.toString())
            }
        )

        else -> asA<T>(compact(obj["result"] ?: nullNode()))
    }
}

