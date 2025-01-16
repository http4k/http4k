package org.http4k.mcp

import org.http4k.connect.mcp.HasMethod
import org.http4k.connect.mcp.ServerMessage
import org.http4k.connect.mcp.ServerMessage.Notification
import org.http4k.connect.mcp.ServerMessage.Request
import org.http4k.connect.mcp.ServerMessage.Response
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.renderError
import org.http4k.format.renderNotification
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.sse.SseMessage.Event

class Serde<NODE : Any>(val json: AutoMarshallingJson<NODE>) {
    inline operator fun <reified OUT : Any> invoke(input: JsonRpcRequest<NODE>): OUT = with(json) {
        asA<OUT>(compact(input.params ?: obj()))
    }

    inline operator fun <reified OUT : Any> invoke(input: JsonRpcResult<NODE>): OUT = with(json) {
        asA<OUT>(compact(input.result ?: json.nullNode()))
    }

    operator fun invoke(method: HasMethod, input: ServerMessage, id: NODE?) = with(json) {
        Event(
            "message",
            compact(
                when (input) {
                    is Request -> renderRequest(method.Method.value, asJsonObject(input), id ?: json.nullNode())
                    is Response -> renderResult(asJsonObject(input), id ?: json.nullNode())
                    is Notification -> renderNotification(input.method.value)
                }
            )
        )
    }

    operator fun invoke(errorMessage: ErrorMessage, id: NODE?) = with(json) {
        Event("message", compact(renderError(errorMessage, id)), id?.let(::asFormatString))
    }
}
