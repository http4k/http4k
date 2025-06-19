package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpProgress : McpRpc {
    override val Method = of("notifications/progress")

    @JsonSerializable
    data class Notification(
        val progressToken: ProgressToken,
        val progress: Int,
        val total: Double?,
        val description: String?,
        override val _meta: Meta = Meta.default
    ) : ServerMessage.Notification, ClientMessage.Notification, HasMeta
}
