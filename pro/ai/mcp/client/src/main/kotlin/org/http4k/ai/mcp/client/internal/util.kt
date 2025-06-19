package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.ai.mcp.McpError.Internal
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

internal inline fun <reified T : Any> McpNodeType.asOrFailure() = with(McpJson) {
    val obj = this@asOrFailure as MoshiObject
    val error = obj["error"]
    when {
        error != null -> Failure(
            Protocol(
                when (error) {
                    is MoshiObject -> ErrorMessage(
                        error["code"]?.let { integer(it).toInt() } ?: -1,
                        error["message"]?.let { text(it) } ?: "Unknown error"
                    )

                    else -> ErrorMessage(-1, error.toString())
                }
            ))

        else -> resultFrom { asA<T>(compact(obj["result"] ?: nullNode())) }
            .flatMapFailure { Failure(Internal(it)) }
    }
}

