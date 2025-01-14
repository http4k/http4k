package org.http4k.mcp

import org.http4k.connect.mcp.ClientMessage.Request
import org.http4k.connect.mcp.HasMethod
import org.http4k.connect.mcp.ServerMessage
import org.http4k.connect.mcp.ServerMessage.Response
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.sse.Sse

class Session<NODE : Any>(
    val id: SessionId,
    val serDe: Serde<NODE>,
    val sse: Sse
) {
    inline fun <reified IN : Request, OUT : Response> process(
        op: HasMethod,
        req: JsonRpcRequest<NODE>,
        fn: (IN) -> OUT
    ) = sse.send(
        runCatching { serDe<IN>(req) }
            .mapCatching(fn)
            .map { serDe(op.Method, it, req.id) }
            .recover { serDe(InternalError, req.id) }
            .getOrElse { serDe(InvalidRequest, req.id) }
    )

    fun send(hasMethod: HasMethod, resp: ServerMessage, id: NODE? = null) = sse.send(serDe(hasMethod.Method, resp, id))
}
