package org.http4k.mcp.server

import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.protocol.ClientMessage.Request
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.protocol.ServerMessage.Notification
import org.http4k.mcp.protocol.ServerMessage.Response
import org.http4k.sse.Sse

class ClientSession<NODE : Any>(val sse: Sse, val handler: McpMessageHandler<NODE>) {
    inline fun <reified IN : Request, OUT : Response> send(req: JsonRpcRequest<NODE>, fn: (IN) -> OUT) =
        sse.send(handler(req, fn))

    fun send(hasMethod: HasMethod, req: ServerMessage.Request, id: NODE? = null) = sse.send(
        handler(hasMethod, req, id)
    )

    fun send(resp: Response, id: NODE? = null) = sse.send(handler(resp, id))
    fun send(notification: Notification) = sse.send(handler(notification))
}
