package org.http4k.mcp.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.client.McpError
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.MessageId
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import java.util.concurrent.atomic.AtomicReference

inline fun <reified T : Any, OUT> AtomicReference<TestSseClient>.nextEvent(fn: T.() -> OUT): McpResult<Pair<MessageId?, OUT>> {

    val fields = McpJson.fields(McpJson.parse((get().received().first() as SseMessage.Event).data)).toMap()

    return when {
        fields["error"] != null -> Failure(
            McpError.Protocol(McpJson.convert<McpNodeType, ErrorMessage>(fields["error"]!!))
        )

        else -> Success(
            fields["id"]?.let { McpJson.convert<MoshiNode, MessageId>(it) }
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

inline fun <reified T : Any> AtomicReference<TestSseClient>.nextNotification(mcpRpc: McpRpc): T {
    val jsonRpcRequest = JsonRpcRequest(
        McpJson,
        McpJson.fields(McpJson.parse((get().received().first() as SseMessage.Event).data)).toMap()
    )

    require(mcpRpc.Method.value == jsonRpcRequest.method) {
        "Expected ${mcpRpc.Method.value} but got ${jsonRpcRequest.method}"
    }

    return McpJson.convert<McpNodeType, T>(jsonRpcRequest.params ?: McpJson.nullNode())
}
