package org.http4k.mcp.server

import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.protocol.ClientMessage.Request
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.protocol.ServerMessage.Notification
import org.http4k.mcp.protocol.ServerMessage.Response
import org.http4k.sse.Sse

class ClientSession<NODE : Any>(
    val id: SessionId,
    val serDe: Serde<NODE>,
    val sse: Sse
) {
    inline fun <reified IN : Request, OUT : Response> process(
        req: JsonRpcRequest<NODE>,
        fn: (IN) -> OUT
    ) = sse.send(
        runCatching { serDe<IN>(req) }
            .mapCatching(fn)
            .map { serDe(it, req.id) }
            .recover { serDe(InternalError, req.id) }
            .getOrElse { serDe(InvalidRequest, req.id) }
            .also { println(it) }
    )

    fun send(hasMethod: HasMethod, req: ServerMessage.Request, id: NODE? = null) = sse.send(serDe(hasMethod, req, id))
    fun send(resp: Response, id: NODE? = null) = sse.send(serDe(resp, id))
    fun send(notification: Notification) = sse.send(serDe(notification))
}
