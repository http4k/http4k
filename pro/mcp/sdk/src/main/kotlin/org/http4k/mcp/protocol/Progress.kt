package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object Progress {
    @JsonSerializable
    data class Request(val progress: Int, val total: Double?) : ClientMessage.Request

    @JsonSerializable
    data class Response(val progress: Int, val total: Double?) : ServerMessage.Response

    @JsonSerializable
    data object Notification : ServerMessage.Notification {
        override val method = of("notifications/progress")
    }
}
