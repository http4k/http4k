package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.ToolAnnotations
import org.http4k.ai.model.ToolName
import org.http4k.format.MoshiNode
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpTool(
    val name: ToolName,
    val description: String,
    val title: String?,
    val inputSchema: Map<String, Any>,
    val outputSchema: Map<String, Any>?,
    val annotations: ToolAnnotations?,
) {
    object List : McpRpc {
        override val Method = of("tools/list")

        @JsonSerializable
        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, HasMeta, PaginatedRequest

        @JsonSerializable
        data class Response(
            val tools: kotlin.collections.List<McpTool>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta

        data object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/tools/list_changed")

            @JsonSerializable
            data object Notification : ServerMessage.Notification
        }
    }

    object Call : McpRpc {
        override val Method = of("tools/call")

        @JsonSerializable
        data class Request(
            val name: ToolName,
            val arguments: Map<String, MoshiNode> = emptyMap(),
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val content: kotlin.collections.List<Content>?,
            val structuredContent: Map<String, Any>? = null,
            val isError: Boolean? = false,
            override val _meta: Meta = Meta.default,
        ) : ServerMessage.Response, HasMeta
    }
}
