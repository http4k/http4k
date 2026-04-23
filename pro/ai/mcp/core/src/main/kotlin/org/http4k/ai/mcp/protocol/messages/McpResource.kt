/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
import se.ansman.kotshi.PolymorphicLabel

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
    val _meta: Meta = Meta.default
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
        _meta: Meta = Meta.default
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
        _meta: Meta = Meta.default
    ) : this(null, uriTemplate, name, description, title, mimeType, size, annotations, icons, _meta)

    object Read : McpRpc {
        override val Method = of("resources/read")

        @JsonSerializable
        @PolymorphicLabel("resources/read")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Read.Method

            @JsonSerializable
            data class Params(
                val uri: Uri,
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val contents: kotlin.collections.List<Resource.Content>,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Response, HasMeta
        }
    }

    object List : McpRpc {
        override val Method = of("resources/list")

        @JsonSerializable
        @PolymorphicLabel("resources/list")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = List.Method

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : PaginatedRequest, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val resources: kotlin.collections.List<McpResource>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Response, PaginatedResponse, HasMeta
        }

        data object Changed : McpRpc {
            override val Method: McpRpcMethod = of("notifications/resources/list_changed")

            @JsonSerializable
            @PolymorphicLabel("notifications/resources/list_changed")
            data class Notification(val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
                override val method = Changed.Method

                @JsonSerializable
                data class Params(override val _meta: Meta = Meta.default) : ServerMessage.Notification
            }
        }
    }

    object ListTemplates : McpRpc {
        override val Method = of("resources/templates/list")

        @JsonSerializable
        @PolymorphicLabel("resources/templates/list")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = ListTemplates.Method

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, PaginatedRequest, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val resourceTemplates: kotlin.collections.List<McpResource>,
                override val nextCursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Response, PaginatedResponse, HasMeta
        }
    }

    data object Updated : McpRpc {
        override val Method: McpRpcMethod = of("notifications/resources/updated")

        @JsonSerializable
        @PolymorphicLabel("notifications/resources/updated")
        data class Notification(val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Updated.Method

            @JsonSerializable
            data class Params(val uri: Uri, override val _meta: Meta = Meta.default) : ServerMessage.Notification,
                HasMeta
        }
    }

    object Subscribe : McpRpc {
        override val Method = of("resources/subscribe")

        @JsonSerializable
        @PolymorphicLabel("resources/subscribe")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Subscribe.Method

            @JsonSerializable
            data class Params(
                val uri: Uri,
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta
        }
    }

    object Unsubscribe : McpRpc {
        override val Method = of("resources/unsubscribe")

        @JsonSerializable
        @PolymorphicLabel("resources/unsubscribe")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Unsubscribe.Method

            @JsonSerializable
            data class Params(
                val uri: Uri,
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta
        }
    }

}
