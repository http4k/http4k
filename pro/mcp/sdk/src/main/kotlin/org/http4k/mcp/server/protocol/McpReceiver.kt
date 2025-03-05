package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId

interface McpReceiver<RSP : Any> {
    fun receive(sId: SessionId, request: Request): RSP
}
