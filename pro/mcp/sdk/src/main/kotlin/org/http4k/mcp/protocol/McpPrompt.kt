package org.http4k.mcp.protocol

import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpPrompt(val name: String, val description: String?, val arguments: kotlin.collections.List<Argument>) {
    @JsonSerializable
    data class Argument(val name: String, val description: String? = null, val required: Boolean? = null)

    object Get : HasMethod {
        override val Method = of("prompts/get")

        @JsonSerializable
        data class Request(
            val name: String,
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

    object List : HasMethod {
        override val Method = of("prompts/list")

        @JsonSerializable
        data class Request(override val _meta: Meta = default) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val prompts: kotlin.collections.List<McpPrompt>,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta

        @JsonSerializable
        data class Changed(override val method: McpRpcMethod = of("notifications/prompts/list_changed")) :
            ServerMessage.Notification
    }
}
