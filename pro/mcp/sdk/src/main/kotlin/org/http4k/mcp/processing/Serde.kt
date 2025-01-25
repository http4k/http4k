package org.http4k.mcp.processing

import org.http4k.format.renderError
import org.http4k.format.renderNotification
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.ServerMessage.Notification
import org.http4k.mcp.protocol.ServerMessage.Request
import org.http4k.mcp.protocol.ServerMessage.Response
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage.Event

object Serde {
    inline operator fun <reified OUT : Any> invoke(input: JsonRpcRequest<McpNodeType>): OUT = with(McpJson) {
        asA<OUT>(asFormatString(input.params ?: obj()))
    }

    inline operator fun <reified OUT : Any> invoke(input: JsonRpcResult<McpNodeType>): OUT = with(McpJson) {
        asA<OUT>(compact(input.result ?: nullNode()))
    }

    operator fun invoke(method: HasMethod, input: Request, id: McpNodeType?) = with(McpJson) {
        Event("message", compact(renderRequest(method.Method.value, asJsonObject(input), id ?: McpJson.nullNode())))
    }

    operator fun invoke(input: Response, id: McpNodeType?) = with(McpJson) {
        Event("message", compact(renderResult(asJsonObject(input), id ?: McpJson.nullNode())))
    }

    operator fun invoke(input: Notification) = with(McpJson) {
        Event("message", compact(renderNotification(input)))
    }

    operator fun invoke(errorMessage: ErrorMessage, id: McpNodeType?) = with(McpJson) {
        Event("message", compact(renderError(errorMessage, id)))
    }
}
