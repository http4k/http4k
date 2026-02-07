package org.http4k.ai.mcp.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
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

internal inline fun <reified T : Any> Sequence<SseMessage.Event>.nextNotification(mcpRpc: McpRpc): T {
    val request = this
        .map { McpJson.fields(McpJson.parse(it.data)).toMap() }
        .filter { it.containsKey("method") }
        .filter { McpJson.text(it["method"]!!) == mcpRpc.Method.value }
        .map { JsonRpcRequest(McpJson, it) }
        .lastOrNull()

    require(request != null) { "Expected ${mcpRpc.Method.value}" }

    return McpJson.convert<McpNodeType, T>(request.params ?: McpJson.nullNode())
}

fun org.http4k.ai.mcp.testing.TestMcpClient.useClient(fn: org.http4k.ai.mcp.testing.TestMcpClient.() -> Unit) {
    use {
        it.start()
        it.fn()
    }
}
