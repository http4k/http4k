package org.http4k.connect.mcp

import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Root

object McpRoot {
    object List : HasMethod {
        override val Method = McpRpcMethod.of("roots/list")

        data class Request(override val _meta: Meta = HasMeta.default) : ServerMessage.Request, HasMeta

        data class Response(val roots: kotlin.collections.List<Root>, override val _meta: Meta = HasMeta.default) :
            ClientMessage.Response, HasMeta
    }

    object Changed : ClientMessage.Notification, HasMethod {
        override val Method = McpRpcMethod.of("notifications/roots/list_changed")
    }
}
