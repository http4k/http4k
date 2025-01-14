package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.core.Uri

data class Root(val uri: Uri, val name: String?) {
    object List : HasMethod {
        override val Method = of("roots/list")

        data class Request(override val _meta: Meta = default) : ServerMessage.Request, HasMeta

        data class Response(val roots: kotlin.collections.List<Root>, override val _meta: Meta = default) :
            ClientMessage.Response, HasMeta
    }

    object Notification : ClientMessage.Notification, HasMethod {
        override val Method = of("notifications/roots/list_changed")
    }
}
