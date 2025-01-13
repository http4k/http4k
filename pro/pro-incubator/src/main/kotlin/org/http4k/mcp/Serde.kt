package org.http4k.mcp

import org.http4k.connect.mcp.ServerResponse
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.renderError
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.sse.SseMessage
import java.util.concurrent.atomic.AtomicInteger

class Serde<NODE : Any>(val json: AutoMarshallingJson<NODE>) {
    private val id = AtomicInteger(0)

    inline operator fun <reified OUT : Any> invoke(input: JsonRpcRequest<NODE>) = with(json) {
        asA<OUT>(compact(input.params ?: obj()))
    }

    operator fun invoke(response: ServerResponse) = with(json) {
        SseMessage.Event("message", compact(renderResult(asJsonObject(response), asJsonObject(id.incrementAndGet()))))
    }

    operator fun invoke(errorMessage: ErrorMessage) = with(json) {
        SseMessage.Event("message", compact(renderError(errorMessage, asJsonObject(id.incrementAndGet()))))
    }
}
