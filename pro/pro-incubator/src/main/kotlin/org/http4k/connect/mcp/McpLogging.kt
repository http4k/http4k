package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.mcp.model.Meta

object McpLogging {
    enum class Level {
        debug, info, notice, warning, error, critical, alert, emergency;
    }

    object SetLevel : HasMethod {
        override val Method = McpRpcMethod.of("logging/set_level")

        data class Request(val level: Level, override val _meta: Meta = default) :
            ClientMessage.Request,
            HasMeta
    }

    object LoggingMessage : HasMethod {
        override val Method = McpRpcMethod.of("notifications/message")

        data class Response(
            val level: Level,
            val logger: String? = null,
            val data: Map<String, Any> = emptyMap(),
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta
    }

}
