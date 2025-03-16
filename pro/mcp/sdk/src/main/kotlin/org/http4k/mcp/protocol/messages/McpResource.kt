package org.http4k.mcp.protocol.messages

import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.mcp.model.Cursor
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpResource(
    val uri: Uri?,
    val uriTemplate: Uri?,
    val name: ResourceName,
    val description: String?,
    val mimeType: MimeType?
) {

    object Read : McpRpc {
        override val Method = of("resources/read")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val contents: kotlin.collections.List<Resource.Content>,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta
    }

    object List : McpRpc {
        override val Method = of("resources/list")

        @JsonSerializable
        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = default
        ) : PaginatedRequest, HasMeta

        @JsonSerializable
        data class Response(
            val resources: kotlin.collections.List<McpResource>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta

        data object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/resources/list_changed")

            @JsonSerializable
            data object Notification : ServerMessage.Notification
        }
    }

    data object Updated : McpRpc {
        override val Method: McpRpcMethod = of("notifications/resources/list_changed")

        @JsonSerializable
        data class Notification(val uri: Uri, override val _meta: Meta = default) : ServerMessage.Notification, HasMeta
    }

    object Subscribe : McpRpc {
        override val Method = of("resources/subscribe")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta
    }

    object Unsubscribe : McpRpc {
        override val Method = of("resources/unsubscribe")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta
    }

    object Template {
        object List : McpRpc {
            override val Method = of("resources/templates/list")

            @JsonSerializable
            data class Request(
                override val cursor: Cursor? = null,
                override val _meta: Meta = default
            ) : ClientMessage.Request, PaginatedRequest, HasMeta {}

            @JsonSerializable
            data class Response(
                val resourceTemplates: kotlin.collections.List<McpResource>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = default
            ) : ServerMessage.Response, PaginatedResponse, HasMeta
        }
    }
}
