package org.http4k.mcp.protocol

import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Root
import se.ansman.kotshi.JsonSerializable

object McpRoot {
    object List : HasMethod {
        override val Method = McpRpcMethod.of("roots/list")

        @JsonSerializable
        data class Request(override val _meta: Meta = HasMeta.default) : ServerMessage.Request, HasMeta

        @JsonSerializable
        data class Response(val roots: kotlin.collections.List<Root>, override val _meta: Meta = HasMeta.default) :
            ClientMessage.Response, HasMeta
    }

    @JsonSerializable
    data class Changed(override val method: McpRpcMethod = Method) : ClientMessage.Notification {
        companion object : HasMethod {
            override val Method = McpRpcMethod.of("notifications/roots/list_changed")
        }
    }
}
