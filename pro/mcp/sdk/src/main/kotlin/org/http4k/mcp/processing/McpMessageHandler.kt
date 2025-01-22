package org.http4k.mcp.processing

import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.server.Serde

class McpMessageHandler<NODE : Any>(val serDe: Serde<NODE>) {
    inline operator fun <reified IN : ClientMessage.Request, OUT : ServerMessage.Response>
        invoke(req: JsonRpcRequest<NODE>, fn: (IN) -> OUT) =
        runCatching { serDe<IN>(req) }
            .mapCatching(fn)
            .map { serDe(it, req.id) }
            .recover { serDe(ErrorMessage.InternalError, req.id) }
            .getOrElse { serDe(ErrorMessage.InvalidRequest, req.id) }

    operator fun invoke(hasMethod: HasMethod, req: ServerMessage.Request, id: NODE? = null) = serDe(hasMethod, req, id)
    operator fun invoke(resp: ServerMessage.Response, id: NODE? = null) = serDe(resp, id)
    operator fun invoke(notification: ServerMessage.Notification) = serDe(notification)
}
