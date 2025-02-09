package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpPrompt(val name: PromptName, val description: String?, val arguments: kotlin.collections.List<Argument>) {
    @JsonSerializable
    data class Argument(val name: String, val description: String? = null, val required: Boolean? = null)

    object Get : McpRpc {
        override val Method = of("prompts/get")

        @JsonSerializable
        data class Request(
            val name: PromptName,
            val arguments: Map<String, String> = emptyMap(),
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val messages: kotlin.collections.List<Message>,
            val description: String? = null,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta
    }

    object List : McpRpc {
        override val Method = of("prompts/list")

        @JsonSerializable
        data class Request(override val _meta: Meta = default) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val prompts: kotlin.collections.List<McpPrompt>,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta

        object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/prompts/list_changed")

            @JsonSerializable
            data object Notification : ServerMessage.Notification
        }
    }
}
