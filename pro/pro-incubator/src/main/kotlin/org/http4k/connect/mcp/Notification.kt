package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default

object Notification {
    data class Cancelled(
        val requestId: String,
        val reason: String?,
        override val _meta: Meta = default
    ) : ServerResponse, HasMeta {
        companion object : HasMethod {
            override val method = McpRpcMethod.of("notifications/cancelled")
        }
    }

    data class LoggingMessage(
        val level: LoggingLevel,
        val logger: String? = null,
        val data: Map<String, Any> = emptyMap(),
        override val _meta: Meta = default
    ) : ServerResponse, HasMeta {
        companion object : HasMethod {
            override val method = McpRpcMethod.of("notifications/message")
        }
    }
}

object Logging {

    enum class Level {
        debug,
        info,
        notice,
        warning,
        error,
        critical,
        alert,
        emergency,
        ;
    }

    object SetLevel {
        data class Request(val level: LoggingLevel, override val _meta: Meta = default) : ClientRequest, HasMeta {
            companion object : HasMethod {
                override val method = McpRpcMethod.of("logging/set_level")
            }
        }
    }
}
