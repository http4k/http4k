package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpProgress : HasMethod {
    override val Method = of("notifications/progress")

    @JsonSerializable
    data class Notification(
        val progress: Int,
        val total: Double?,
        val progressToken: ProgressToken,
        override val _meta: Meta = HasMeta.default
    ) : ServerMessage.Notification, ClientMessage.Notification, HasMeta
}
