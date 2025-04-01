package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Root
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpRoot {
    object List : McpRpc {
        override val Method = McpRpcMethod.of("roots/list")

        @JsonSerializable
        data class Request(override val _meta: Meta = Meta.default) : ServerMessage.Request, HasMeta

        @JsonSerializable
        data class Response(val roots: kotlin.collections.List<Root>, override val _meta: Meta = Meta.default) :
            ClientMessage.Response, HasMeta
    }

    data object Changed : McpRpc {
        override val Method: McpRpcMethod = of("notifications/roots/list_changed")

        @JsonSerializable
        data object Notification : ClientMessage.Notification
    }
}
