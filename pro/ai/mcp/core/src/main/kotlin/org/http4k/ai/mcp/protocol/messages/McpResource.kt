package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Annotations
import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Icon
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.ResourceUriTemplate
import org.http4k.ai.mcp.model.Size
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@ExposedCopyVisibility
data class McpResource internal constructor(
    val uri: Uri?,
    val uriTemplate: ResourceUriTemplate?,
    val name: ResourceName,
    val description: String?,
    val title: String?,
    val mimeType: MimeType?,
    val size: Size?,
    val annotations: Annotations?,
    val icons: kotlin.collections.List<Icon>? = null,
    val _meta: Meta? = null
) {
    constructor(
        uri: Uri,
        name: ResourceName,
        description: String? = null,
        mimeType: MimeType? = null,
        size: Size? = null,
        annotations: Annotations? = null,
        title: String? = null,
        icons: kotlin.collections.List<Icon>? = null,
        _meta: Meta? = null
    ) : this(uri, null, name, description, title, mimeType, size, annotations, icons, _meta)

    constructor(
        uriTemplate: ResourceUriTemplate,
        name: ResourceName,
        description: String? = null,
        mimeType: MimeType? = null,
        size: Size? = null,
        annotations: Annotations? = null,
        title: String? = null,
        icons: kotlin.collections.List<Icon>? = null,
        _meta: Meta? = null
    ) : this(null, uriTemplate, name, description, title, mimeType, size, annotations, icons, _meta)

    object Read : McpRpc {
        override val Method = of("resources/read")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, HasMeta

        @JsonSerializable
        data class Response(
            val contents: kotlin.collections.List<Resource.Content>,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Response, HasMeta
    }

    object List : McpRpc {
        override val Method = of("resources/list")

        @JsonSerializable
        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : PaginatedRequest, HasMeta

        @JsonSerializable
        data class Response(
            val resources: kotlin.collections.List<McpResource>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta

        data object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/resources/list_changed")

            @JsonSerializable
            data object Notification : ServerMessage.Notification
        }
    }

    object ListTemplates : McpRpc {
        override val Method = of("resources/templates/list")

        @JsonSerializable
        data class Request(
            override val cursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, PaginatedRequest, HasMeta

        @JsonSerializable
        data class Response(
            val resourceTemplates: kotlin.collections.List<McpResource>,
            override val nextCursor: Cursor? = null,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Response, PaginatedResponse, HasMeta
    }

    data object Updated : McpRpc {
        override val Method: McpRpcMethod = of("notifications/resources/updated")

        @JsonSerializable
        data class Notification(val uri: Uri, override val _meta: Meta = Meta.default) : ServerMessage.Notification,
            HasMeta
    }

    object Subscribe : McpRpc {
        override val Method = of("resources/subscribe")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, HasMeta
    }

    object Unsubscribe : McpRpc {
        override val Method = of("resources/unsubscribe")

        @JsonSerializable
        data class Request(
            val uri: Uri,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, HasMeta
    }

}
