package org.http4k.mcp

import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.ServerRequest
import org.http4k.connect.mcp.ServerResponse
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event

class Serde<NODE : Any>(val json: AutoMarshallingJson<NODE>) {
    inline operator fun <reified OUT : Any> invoke(input: JsonRpcRequest<NODE>): OUT = with(json) {
        asA<OUT>(compact(input.params ?: obj()))
    }

    operator fun invoke(response: ServerResponse, id: NODE?) = with(json) {
        SseMessage.Event(
            "message",
            compact(renderResult(asJsonObject(response), id ?: nullNode())),
            asFormatString(id ?: nullNode())
        )
    }

    operator fun invoke(errorMessage: ErrorMessage, id: NODE?) = with(json) {
        SseMessage.Event("message", compact(renderError(errorMessage, id)), id?.let(::asFormatString))
    }

    operator fun invoke(method: McpRpcMethod, input: ServerRequest, id: NODE) = with(json) {
        Event(
            "message",
            compact(renderRequest(method.value, asJsonObject(input), id)),
            asFormatString(id)
        )
    }

}
