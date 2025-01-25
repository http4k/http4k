package org.http4k.mcp.processing

import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.util.McpNodeType

/**
 * Handles MCP processing and transforming from and ro JSON RPC messages
 */
object McpMessageHandler {

    inline operator fun <reified IN : ClientMessage.Request>
        invoke(req: JsonRpcRequest<McpNodeType>, fn: (IN) -> ServerMessage.Response) =
        runCatching { Serde<IN>(req) }
            .mapCatching(fn)
            .map { Serde(it, req.id) }
            .recover { Serde(ErrorMessage.InternalError, req.id) }
            .getOrElse { Serde(ErrorMessage.InvalidRequest, req.id) }

    operator fun invoke(hasMethod: HasMethod, req: ServerMessage.Request, id: McpNodeType? = null) =
        Serde(hasMethod, req, id)

    operator fun invoke(resp: ServerMessage.Response, id: McpNodeType? = null) = Serde(resp, id)
    operator fun invoke(notification: ServerMessage.Notification) = Serde(notification)
}
