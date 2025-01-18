package org.http4k.connect.mcp

import org.http4k.connect.mcp.McpRpcMethod.Companion.of

object Progress {
    data class Request(val progress: Int, val total: Double?) : ClientMessage.Request
    data class Response(val progress: Int, val total: Double?) : ServerMessage.Response

    object Notification : ServerMessage.Notification {
        override val method = of("notifications/progress")
    }
}

