package org.http4k.mcp.protocol

import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpProgress {
    @JsonSerializable
    data class Notification(
        val progress: Int,
        val total: Double?,
        val progressToken: ProgressToken,
        override val _meta: Meta = HasMeta.default
    ) : ServerMessage.Notification, ClientMessage.Notification, HasMeta {
        override val method = Method

        companion object : HasMethod {
            override val Method = of("notifications/progress")
        }
    }
}
