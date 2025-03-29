package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.lens.contentType
import org.http4k.mcp.client.McpError.Protocol
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage.Event

internal inline fun <reified T : ServerMessage> Event.asAOrFailure(): Result<T, Protocol> = with(McpJson) {
    val data = parse(data) as MoshiObject

    when {
        data["method"] != null -> Failure(Protocol(InvalidRequest))
        else -> {
            resultFrom {
                convert<MoshiNode, T>(data.attributes["result"] ?: nullNode())
            }.mapFailure { Protocol(ErrorMessage(-1, it.toString())) }
        }
    }
}

internal fun ClientMessage.toHttpRequest(endpoint: Uri, rpc: McpRpc, messageId: McpMessageId? = null) =
    Request(POST, endpoint)
        .contentType(APPLICATION_JSON)
        .body(with(McpJson) {
            val params = asJsonObject(this@toHttpRequest)
            val id = messageId?.let { asJsonObject(it) } ?: nullNode()
            compact(
                when (this@toHttpRequest) {
                    is ClientMessage.Response -> renderResult(params, id)
                    else -> renderRequest(rpc.Method.value, params, id)
                }
            )
        })
