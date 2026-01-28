package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable

object McpLogging {
    object SetLevel : McpRpc {
        override val Method = McpRpcMethod.of("logging/setLevel")

        @JsonSerializable
        data class Request(
            val level: LogLevel,
            override val _meta: Meta = Meta.default
        ) :
            ClientMessage.Request,
            HasMeta
    }

    object LoggingMessage : McpRpc {
        override val Method = McpRpcMethod.of("notifications/message")

        @JsonSerializable
        data class Notification(
            val data: McpNodeType,
            val level: LogLevel,
            val logger: String? = null,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Notification, HasMeta
    }
}
