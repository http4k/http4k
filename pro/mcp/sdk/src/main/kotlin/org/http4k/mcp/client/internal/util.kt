package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.resultFrom
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

internal inline fun <reified T : Any> McpNodeType.asOrFailure() = with(McpJson) {
    val obj = this@asOrFailure as MoshiObject
    val error = obj["error"]
    when {
        error != null -> Failure(
            McpException(
                when (error) {
                    is MoshiObject -> ErrorMessage(
                        error["code"]?.let { integer(it).toInt() } ?: -1,
                        error["message"]?.let { text(it) } ?: "Unknown error"
                    )

                    else -> ErrorMessage(-1, error.toString())
                }
            )
        )

        else -> resultFrom { asA<T>(compact(obj["result"] ?: nullNode())) }
    }
}

