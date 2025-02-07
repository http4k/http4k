package org.http4k.mcp.protocol

import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.HasMeta.Companion.default

data class Cancelled(
    val requestId: String,
    val reason: String?,
    override val _meta: Meta = default
) : ClientMessage.Notification, ServerMessage.Notification, HasMeta {
    override val method = Method

    companion object : HasMethod {
        override val Method = McpRpcMethod.of("notifications/cancelled")
    }
}
