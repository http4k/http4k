package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default

object Notification {
    data class Cancelled(
        val requestId: String,
        val reason: String?,
        override val _meta: Meta = default
    ) : ServerResponse, HasMeta {
        companion object : HasMethod {
            override val Method = McpRpcMethod.of("notifications/cancelled")
        }
    }

    data class LoggingMessage(
        val level: Logging.Level,
        val logger: String? = null,
        val data: Map<String, Any> = emptyMap(),
        override val _meta: Meta = default
    ) : ServerResponse, HasMeta {
        companion object : HasMethod {
            override val Method = McpRpcMethod.of("notifications/message")
        }
    }
}

