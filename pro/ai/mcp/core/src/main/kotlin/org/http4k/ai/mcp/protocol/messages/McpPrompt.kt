/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Icon
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
data class McpPrompt(
    val name: PromptName,
    val description: String?,
    val title: String?,
    val arguments: kotlin.collections.List<Argument>,
    val icons: kotlin.collections.List<Icon>? = null
) {
    @JsonSerializable
    data class Argument(
        val name: String,
        val description: String? = null,
        val title: String? = null,
        val required: Boolean? = null
    )

    object Get : McpRpc {
        override val Method = of("prompts/get")

        @JsonSerializable
        @PolymorphicLabel("prompts/get")
        data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
            @JsonSerializable
            data class Params(
                val name: PromptName,
                val arguments: Map<String, String> = emptyMap(),
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: McpNodeType?) : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val messages: kotlin.collections.List<Message>,
                val description: String? = null,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Response, HasMeta
        }
    }

    object List : McpRpc {
        override val Method = of("prompts/list")

        @JsonSerializable
        @PolymorphicLabel("prompts/list")
        data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
            @JsonSerializable
            data class Params(
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: McpNodeType?) : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val prompts: kotlin.collections.List<McpPrompt>,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Response, HasMeta
        }

        object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/prompts/list_changed")

            @JsonSerializable
            @PolymorphicLabel("notifications/prompts/list_changed")
            data class Notification(val params: Params, override val id: McpNodeType? = null) : McpJsonRpcRequest() {
                @JsonSerializable
                data class Params(override val _meta: Meta = Meta.default) : ServerMessage.Notification
            }
        }
    }
}
