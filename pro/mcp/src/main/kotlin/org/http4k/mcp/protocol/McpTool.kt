package org.http4k.mcp.protocol

import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Cursor
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of

data class McpTool(val name: String, val description: String, val inputSchema: Map<String, Any> = emptyMap()) {
    object List : HasMethod {
        override val Method = of("tools/list")

        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta, PaginatedRequest

        data class Response(
            val tools: kotlin.collections.List<McpTool>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta

        data object Changed : ServerMessage.Notification {
            override val method = of("notifications/tools/list_changed")
        }
    }

    object Call : HasMethod {
        override val Method = of("tools/call")

        data class Request(
            val name: String,
            val arguments: Map<String, Any> = emptyMap(),
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        class Response(
            val content: kotlin.collections.List<Content>,
            val isError: Boolean? = false,
            override val _meta: Meta = default,
        ) : ServerMessage.Response, HasMeta
    }
}
