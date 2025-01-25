package org.http4k.mcp.protocol

import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Cancelled(
    val requestId: String,
    val reason: String?,
    override val _meta: Meta = default,
    override val method: McpRpcMethod = Method
) : ClientMessage.Notification, ServerMessage.Notification, HasMeta {
    companion object : HasMethod {
        override val Method = McpRpcMethod.of("notifications/cancelled")
    }
}
