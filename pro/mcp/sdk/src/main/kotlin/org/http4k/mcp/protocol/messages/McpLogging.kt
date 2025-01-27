package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

object McpLogging {
    object SetLevel : HasMethod {
        override val Method = McpRpcMethod.of("logging/set_level")

        @JsonSerializable
        data class Request(val level: LogLevel, override val _meta: Meta = default) :
            ClientMessage.Request,
            HasMeta
    }

    object LoggingMessage : HasMethod {
        override val Method = McpRpcMethod.of("notifications/message")

        @JsonSerializable
        data class Notification(
            val level: LogLevel,
            val logger: String? = null,
            val data: Map<String, Any> = emptyMap(),
            override val _meta: Meta = default
        ) : ServerMessage.Notification, HasMeta
    }
}
