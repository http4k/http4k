package org.http4k.mcp.processing

import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.HasMethod
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.util.McpNodeType

/**
 * Handles MCP processing and transforming from and to MCP messages
 */
object McpMessageHandler {

    inline operator fun <reified IN : ClientMessage.Request>
        invoke(req: JsonRpcRequest<McpNodeType>, fn: (IN) -> ServerMessage.Response) =
        runCatching { SerDe<IN>(req) }
            .mapCatching(fn)
            .map { SerDe(it, req.id) }
            .recover {
                when (it) {
                    is McpException -> SerDe(it.error, req.id)
                    else -> SerDe(InternalError, req.id)
                }
            }
            .getOrElse { SerDe(InvalidRequest, req.id) }

    operator fun invoke(hasMethod: HasMethod, req: ServerMessage.Request, id: McpNodeType? = null) =
        SerDe(hasMethod, req, id)

    operator fun invoke(resp: ServerMessage.Response, id: McpNodeType? = null) = SerDe(resp, id)

    operator fun invoke(notification: ServerMessage.Notification) = SerDe(notification)
}
