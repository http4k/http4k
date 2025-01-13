package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default

object Cancelled {
    object Notification : HasMethod {
        override val Method = McpRpcMethod.of("notifications/cancelled")

        data class Request(
            val requestId: String,
            val reason: String?,
            override val _meta: Meta = default
        ) : ClientRequest, HasMeta

        data class Response(
            val requestId: String,
            val reason: String?,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta
    }
}
