package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.core.Uri

data class Root(val uri: Uri, val name: String?) {
    object List : HasMethod {
        override val Method = of("roots/list")

        data class Request(override val _meta: Meta = default) : ClientRequest, HasMeta

        data class Response(val roots: kotlin.collections.List<Root>, override val _meta: Meta = default) :
            ServerResponse, HasMeta

        data object Notification : ServerNotification {
            override val method = of("notifications/roots/list_changed")
        }
    }
}

