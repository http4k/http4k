package org.http4k.mcp.protocol

import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of

object McpPrompt {
    data class Argument(
        val name: String,
        val description: String? = null,
        val required: Boolean? = null,
    )

    object Get : HasMethod {
        override val Method = of("prompts/get")

        data class Request(
            val name: String,
            val arguments: Map<String, String> = emptyMap(),
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        class Response(
            val messages: kotlin.collections.List<Message>,
            val description: String? = null,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta
    }

    object List : HasMethod {
        override val Method = of("prompts/list")

        data class Request(override val _meta: Meta = default) : ClientMessage.Request, HasMeta

        data class Response(
            val prompts: kotlin.collections.List<Prompt>,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta

        data object Changed : ServerMessage.Notification {
            override val method = of("notifications/prompts/list_changed")
        }
    }
}
