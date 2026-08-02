/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.CacheScope
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Icon
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ResultType
import org.http4k.ai.mcp.model.ToolAnnotations
import org.http4k.ai.mcp.model.TtlMs
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
    val _meta: Meta = Meta.default
) {
    object List {

        @JsonSerializable
        @PolymorphicLabel("tools/list")
        data class Request(val params: Params? = null, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = of("tools/list")

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta, PaginatedRequest
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val tools: kotlin.collections.List<McpTool>,
                override val nextCursor: Cursor? = null,
                override val ttlMs: TtlMs = TtlMs.of(0),
                override val cacheScope: CacheScope = CacheScope.public,
                override val _meta: Meta = Meta.default
            ) : PaginatedResponse, HasMeta, CacheableResult
        }

        data object Changed {

            @JsonSerializable
            @PolymorphicLabel("notifications/tools/list_changed")
            data class Notification(val params: Params? = null, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
                override val method = of("notifications/tools/list_changed")

                @JsonSerializable
                data class Params(override val _meta: Meta = Meta.default) : HasMeta
            }
        }
    }

    object Call {

        @JsonSerializable
        @PolymorphicLabel("tools/call")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = of("tools/call")

            @JsonSerializable
            data class Params(
                val name: ToolName,
                val arguments: Map<String, MoshiNode> = emptyMap(),
                override val inputResponses: Map<String, McpElicitation.Result>? = null,
                override val requestState: String? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta, HasInputResponses
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val content: kotlin.collections.List<Content>? = null,
                val structuredContent: McpNodeType? = null,
                val isError: Boolean? = false,
                override val resultType: ResultType = ResultType.complete,
                override val inputRequests: Map<String, McpElicitation.Create>? = null,
                override val requestState: String? = null,
                override val _meta: Meta = Meta.default,
            ) : HasMeta, HasInputRequired
        }
    }
}
