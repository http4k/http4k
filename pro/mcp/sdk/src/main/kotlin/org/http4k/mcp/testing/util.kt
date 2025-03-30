package org.http4k.mcp.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.MoshiNode
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.lens.accept
import org.http4k.mcp.client.McpError
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage


fun Request.withMcp(mcpRpc: McpRpc, input: ClientMessage.Request, id: Int) =
    with(McpJson) {
        method(POST)
            .accept(TEXT_EVENT_STREAM)
            .body(compact(renderRequest(mcpRpc.Method.value, asJsonObject(input), number(id))))
    }

fun Request.withMcp(input: ClientMessage.Response, messageId: McpMessageId) =
    with(McpJson) {
        method(POST)
            .accept(TEXT_EVENT_STREAM)
            .body(compact(renderResult(asJsonObject(input), number(messageId.value))))
    }

fun Request.withMcp(mcpRpc: McpRpc, input: ClientMessage.Notification, id: Int) =
    with(McpJson) {
        method(POST)
            .accept(TEXT_EVENT_STREAM)
            .body(compact(renderRequest(mcpRpc.Method.value, asJsonObject(input), number(id))))
    }


inline fun <reified T : Any, OUT> Sequence<SseMessage.Event>.nextEvent(fn: T.() -> OUT): McpResult<Pair<McpMessageId?, OUT>> {

    val fields = McpJson.fields(McpJson.parse(first().data)).toMap()

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

inline fun <reified T : Any> Sequence<SseMessage.Event>.nextNotification(mcpRpc: McpRpc): T {
    val jsonRpcRequest = JsonRpcRequest(
        McpJson,
        McpJson.fields(McpJson.parse(first().data)).toMap()
    )

    require(mcpRpc.Method.value == jsonRpcRequest.method) {
        "Expected ${mcpRpc.Method.value} but got ${jsonRpcRequest.method}"
    }

    return McpJson.convert<McpNodeType, T>(jsonRpcRequest.params ?: McpJson.nullNode())
}
