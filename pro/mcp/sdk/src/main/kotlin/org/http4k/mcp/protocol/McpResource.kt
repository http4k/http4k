package org.http4k.mcp.protocol

import org.http4k.core.Uri
import org.http4k.mcp.model.Cursor
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpResource(
    val uri: Uri?,
    val uriTemplate: Uri?,
    val name: String,
    val description: String?,
    val mimeType: MimeType?
) {

    object Read : HasMethod {
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

    object List : HasMethod {
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

        @JsonSerializable
        data object Changed : ServerMessage.Notification {
            override val method = of("notifications/resources/list_changed")
        }
    }

    @JsonSerializable
    data class Updated(val uri: Uri, override val _meta: Meta = default) : ServerMessage.Notification,
        HasMeta {
        override val method = Method

        companion object : HasMethod {
            override val Method = of("notifications/resources/updated")
        }
    }

    object Subscribe : HasMethod {
        override val Method = of("resources/subscribe")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta
    }

    object Unsubscribe : HasMethod {
        override val Method = of("resources/unsubscribe")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta
    }

    object Template {
        object List : HasMethod {
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
