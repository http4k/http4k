package org.http4k.mcp.server

import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.protocol.ServerMessage.Notification
import org.http4k.sse.Sse

class ClientSseConnection<NODE : Any>(val sse: Sse, val handler: McpMessageHandler<NODE>) {
    fun send(hasMethod: HasMethod, req: ServerMessage.Request, id: NODE? = null) = sse.send(
        handler(hasMethod, req, id)
    )

    fun send(notification: Notification) = sse.send(handler(notification))
}
