package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.connect.model.Base64Blob
import org.http4k.core.Uri
import org.http4k.mcp.MimeType

data class Resource(
    val uri: Uri,
    val name: String,
    val description: String? = null,
    val mimeType: MimeType? = null
) {
    sealed interface Content {
        val uri: Uri
        val mimeType: MimeType?

        data class Text(
            val text: String,
            override val uri: Uri,
            override val mimeType: MimeType? = null
        ) : Content

        data class Blob(
            val blob: Base64Blob,
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content

        data class Unknown(
            override val uri: Uri,
            override val mimeType: MimeType? = null,
        ) : Content
    }


    object Read : HasMethod {
        override val Method = of("resources/read")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientMessage.Request, HasMeta

        class Response(
            val contents: kotlin.collections.List<Content>,
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

        data object Notification : ServerMessage.Notification {
            override val method = of("notifications/resources/list_changed")
        }
    }

    object Updated {
        data class Notification(val uri: Uri, override val _meta: Meta = default) : ServerMessage.Notification,
            HasMeta {
            override val method = of("notifications/resources/updated")
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
            override val Method = McpRpcMethod.of("resources/templates/list")

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
