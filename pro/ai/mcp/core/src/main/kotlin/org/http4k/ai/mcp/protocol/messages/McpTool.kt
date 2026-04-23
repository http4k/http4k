/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Icon
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskMeta
import org.http4k.ai.mcp.model.ToolAnnotations
import org.http4k.ai.mcp.model.ToolExecution
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.ai.model.ToolName
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
data class McpTool(
    val name: ToolName,
    val description: String,
    val title: String?,
    val inputSchema: Map<String, Any>,
    val outputSchema: Map<String, Any>?,
    val annotations: ToolAnnotations?,
    val icons: kotlin.collections.List<Icon>? = null,
    val execution: ToolExecution? = null,
    val _meta: Meta = Meta.default
) {
    object List : McpRpc {
        override val Method = of("tools/list")

        @JsonSerializable
        @PolymorphicLabel("tools/list")
        data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
            override val method = List.Method

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta, PaginatedRequest
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: McpNodeType?) : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val tools: kotlin.collections.List<McpTool>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Response, PaginatedResponse, HasMeta
        }

        data object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/tools/list_changed")

            @JsonSerializable
            @PolymorphicLabel("notifications/tools/list_changed")
            data class Notification(val params: Params, override val id: McpNodeType? = null) : McpJsonRpcRequest() {
                override val method = Changed.Method

                @JsonSerializable
                data class Params(override val _meta: Meta = Meta.default) : ServerMessage.Notification
            }
        }
    }

    object Call : McpRpc {
        override val Method = of("tools/call")

        @JsonSerializable
        @PolymorphicLabel("tools/call")
        data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
            override val method = Call.Method

            @JsonSerializable
            data class Params(
                val name: ToolName,
                val arguments: Map<String, MoshiNode> = emptyMap(),
                override val _meta: Meta = Meta.default,
                val task: TaskMeta? = null
            ) : ClientMessage.Request, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: McpNodeType?) : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val content: kotlin.collections.List<Content>? = null,
                val structuredContent: Map<String, Any>? = null,
                val isError: Boolean? = false,
                val task: Task? = null,
                override val _meta: Meta = Meta.default,
            ) : ServerMessage.Response, HasMeta
        }
    }
}
