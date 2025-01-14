package org.http4k.mcp

import org.http4k.connect.mcp.ClientMessage
import org.http4k.connect.mcp.HasMethod
import org.http4k.connect.mcp.ServerMessage
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.ProcessResult.Fail
import org.http4k.mcp.ProcessResult.Ok
import org.http4k.sse.Sse

class Session<NODE : Any>(
    val id: SessionId,
    val serDe: Serde<NODE>,
    val sse: Sse
) {
    inline fun <reified IN : ClientMessage.Request, OUT : ServerMessage.Response>
        process(hasMethod: HasMethod, req: JsonRpcRequest<NODE>, fn: (IN) -> OUT): ProcessResult {
        runCatching { serDe<IN>(req) }
            .onFailure {
                sse.send(serDe(ErrorMessage.InvalidRequest, req.id))
                return Ok
            }
            .map(fn)
            .map {
                sse.send(serDe(hasMethod.Method, it, req.id))
                return Fail(ACCEPTED)
            }
            .recover {
                sse.send(serDe(ErrorMessage.InternalError, req.id))
                return Fail(SERVICE_UNAVAILABLE)
            }
        return Fail(INTERNAL_SERVER_ERROR)
    }

    fun send(hasMethod: HasMethod, resp: ServerMessage, id: NODE? = null) = sse.send(serDe(hasMethod.Method, resp, id))
}
