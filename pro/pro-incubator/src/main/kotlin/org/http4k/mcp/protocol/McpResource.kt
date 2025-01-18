package org.http4k.mcp.protocol

import org.http4k.core.Uri
import org.http4k.mcp.model.Cursor
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.Resource
import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of

object McpResource {

    object Read : HasMethod {
        override val Method = of("resources/read")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        class Response(
            val contents: kotlin.collections.List<Resource.Content>,
            override val _meta: Meta = default
        ) : ServerMessage.Response, HasMeta
    }

    object List : HasMethod {
        override val Method = of("resources/list")

        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = default
        ) : PaginatedRequest, HasMeta

        class Response(
            val resources: kotlin.collections.List<Resource>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta

        data object Changed : ServerMessage.Notification {
            override val method = of("notifications/resources/list_changed")
        }
    }

    data class Updated(val uri: Uri, override val _meta: Meta = default) : ServerMessage.Notification,
        HasMeta {
        override val method = Method

        companion object : HasMethod {
            override val Method = of("notifications/resources/updated")
        }
    }

    object Subscribe : HasMethod {
        override val Method = of("resources/subscribe")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta
    }

    object Unsubscribe : HasMethod {
        override val Method = of("resources/unsubscribe")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta
    }

    data class Template(
        val uriTemplate: Uri,
        val name: String,
        val description: String? = null,
        val mimeType: MimeType? = null,
    ) {
        object List : HasMethod {
            override val Method = of("resources/templates/list")

            data class Request(
                override val cursor: Cursor?,
                override val _meta: Meta = default
            ) : ClientMessage.Request, PaginatedRequest, HasMeta {}

            class Response(
                val resourceTemplates: kotlin.collections.List<Template>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = default
            ) : ServerMessage.Response, PaginatedResponse, HasMeta
        }
    }
}
