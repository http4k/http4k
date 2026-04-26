/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.sse.SseMessage


inline fun <OUT, reified T : Any> SseMessage.Event.nextEvent(noinline fn: T.() -> OUT): McpResult<Pair<McpMessageId?, OUT>> {
    val fields = McpJson.fields(McpJson.parse(data)).toMap()

    return when {
        fields["error"] != null -> Failure(
            McpError.Protocol(McpJson.convert<McpNodeType, ErrorMessage>(fields["error"]!!))
        )

        else -> Success(
            fields["id"]?.let { McpJson.convert<MoshiNode, McpMessageId>(it) }
                to
                fn(
                    McpJson.convert<McpNodeType, T>(
                        fields["result"] ?: fields["params"]
                        ?: error("No result or params in $fields")
                    )
                )
        )
    }
}

internal inline fun <reified T : McpJsonRpcRequest> SseMessage.Event.toMessage(): T {
    val message = McpJson.asA<McpJsonRpcRequest>(data)
    require(message is T) { "Expected ${T::class.simpleName} but got ${message::class.simpleName}" }
    return message
}

fun TestMcpClient.useClient(fn: TestMcpClient.() -> Unit) {
    use {
        it.start()
        it.fn()
    }
}
