package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Cursor
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

data class McpTool(val name: String, val description: String, val inputSchema: Map<String, Any> = emptyMap()) {
    object List : HasMethod {
        override val Method = of("tools/list")

        @JsonSerializable
        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta, PaginatedRequest

        @JsonSerializable
        data class Response(
            val tools: kotlin.collections.List<McpTool>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta

        @JsonSerializable
        data class Changed(override val method: McpRpcMethod = of("notifications/tools/list_changed")) :
            ServerMessage.Notification
    }

    object Call : HasMethod {
        override val Method = of("tools/call")

        @JsonSerializable
        data class Request(
            val name: String,
            val arguments: Map<String, Any> = emptyMap(),
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val content: kotlin.collections.List<Content>,
            val isError: Boolean? = false,
            override val _meta: Meta = default,
        ) : ServerMessage.Response, HasMeta
    }
}
