package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

object Cancelled : McpRpc {
    override val Method = McpRpcMethod.of("notifications/cancelled")

    @JsonSerializable
    data class Notification(
        val requestId: String,
        val reason: String?,
        override val _meta: Meta = default,
    ) : ClientMessage.Notification, ServerMessage.Notification, HasMeta
}

