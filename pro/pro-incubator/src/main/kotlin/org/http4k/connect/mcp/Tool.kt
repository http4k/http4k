package org.http4k.connect.mcp

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of

data class Tool(val name: String, val description: String, val schema: JsonNode) {
    companion object {
        object List : HasMethod {
            override val method = of("tools/list")

            data class Request(
                override val cursor: Cursor? = null,
                override val _meta: Meta = default
            ) : ClientRequest, HasMeta, PaginatedRequest

            data object Notification : ServerNotification {
                override val method = of("notifications/tools/list_changed")
            }
        }

        object Call : HasMethod {
            override val method = of("tools/call")

            data class Request<T>(
                val name: String,
                val arguments: T,
                val _meta: Unit? = null,
            ) : ClientRequest

            public class Response(
                val tools: kotlin.collections.List<Tool>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = default,
            ) : ServerResponse, PaginatedResponse, HasMeta

        }
    }
}

