package org.http4k.mcp.processing

import org.http4k.format.AutoMarshallingJson
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
import org.http4k.sse.SseMessage.Event

class Serde<NODE : Any>(val json: AutoMarshallingJson<NODE>) {
    inline operator fun <reified OUT : Any> invoke(input: JsonRpcRequest<NODE>): OUT = with(json) {
        asA<OUT>(compact(input.params ?: obj()))
    }

    inline operator fun <reified OUT : Any> invoke(input: JsonRpcResult<NODE>): OUT = with(json) {
        asA<OUT>(compact(input.result ?: json.nullNode()))
    }

    operator fun invoke(method: HasMethod, input: Request, id: NODE?) = with(json) {
        Event("message", compact(renderRequest(method.Method.value, asJsonObject(input), id ?: json.nullNode())))
    }

    operator fun invoke(input: Response, id: NODE?) = with(json) {
        Event("message", compact(renderResult(asJsonObject(input), id ?: json.nullNode())))
    }

    operator fun invoke(input: Notification) = with(json) {
        Event("message", compact(renderNotification(input)))
    }

    operator fun invoke(errorMessage: ErrorMessage, id: NODE?) = with(json) {
        Event("message", compact(renderError(errorMessage, id)), id?.let(::asFormatString))
    }
}
