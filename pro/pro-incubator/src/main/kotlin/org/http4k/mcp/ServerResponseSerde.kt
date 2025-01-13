package org.http4k.mcp

import org.http4k.connect.mcp.ServerResponse
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.renderResult
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.sse.SseMessage
import java.util.concurrent.atomic.AtomicInteger

class ServerResponseSerde<NODE : Any>(val json: AutoMarshallingJson<NODE>) {
    private val id = AtomicInteger(0)

    operator fun invoke(response: ServerResponse) = with(json) {
        SseMessage.Event("message", compact(renderResult(asJsonObject(response), asJsonObject(id.incrementAndGet()))))
    }

    inline operator fun <reified OUT: Any> invoke(input: JsonRpcRequest<NODE>): OUT = json.asA<OUT>(json.asFormatString(input))

}
