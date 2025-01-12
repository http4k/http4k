package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.core.ContentType
import org.http4k.core.Uri


data class Resource(
    val uri: Uri,
    val name: String,
    val description: String? = null,
    val mimeType: ContentType? = null
) {
    object Read : HasMethod {
        override val Method = of("resources/read")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ClientRequest, HasMeta

        class Response(
            val contents: kotlin.collections.List<ResourceContents>,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta
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
        ) : ServerResponse, PaginatedResponse, HasMeta

        data object Notification : ServerNotification {
            override val method = of("notifications/resources/list_changed")
        }
    }

    object Updated : HasMethod {
        override val Method = of("resources/updated")

        data class Notification(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta
    }

    object Subscribe : HasMethod {
        override val Method = of("resources/subscribe")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta
    }

    object Unsubscribe : HasMethod {
        override val Method = of("resources/unsubscribe")

        data class Request(
            val uri: Uri,
            override val _meta: Meta = default
        ) : ServerResponse, HasMeta
    }

    data class Template(
        val uriTemplate: String,
        val name: String,
        val description: String? = null,
        val mimeType: ContentType? = null,
    ) {
        object List : HasMethod {
            override val Method = McpRpcMethod.of("resources/templates/list")

            data class Request(
                override val cursor: Cursor?,
                override val _meta: Meta = default
            ) : ClientRequest, PaginatedRequest, HasMeta {}

            class Response(
                val resourceTemplates: kotlin.collections.List<Template>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = default
            ) : ServerResponse, PaginatedResponse, HasMeta
        }
    }
}
