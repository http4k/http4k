/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.Annotations
import org.http4k.ai.mcp.stateless.model.CacheScope
import org.http4k.ai.mcp.stateless.model.Cursor
import org.http4k.ai.mcp.stateless.model.Icon
import org.http4k.ai.mcp.stateless.model.McpUri
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.ResourceName
import org.http4k.ai.mcp.stateless.model.ResourceUriTemplate
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.model.Size
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod.Companion.of
import org.http4k.connect.model.MimeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@ExposedCopyVisibility
data class McpResource internal constructor(
    val uri: McpUri?,
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
        uri: McpUri,
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

    object Read {

        @JsonSerializable
        @PolymorphicLabel("resources/read")
        data class Request(override val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = of("resources/read")

            @JsonSerializable
            data class Params(
                val uri: McpUri,
                override val inputResponses: Map<String, McpElicitation.Result>? = null,
                override val requestState: String? = null,
                override val _meta: Meta = Meta.default
            ) : HasMeta, HasInputResponses, HasMirroredName {
                override fun mirroredName() = uri.value
            }
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val contents: kotlin.collections.List<Resource.Content>,
                override val resultType: ResultType = ResultType.complete,
                override val inputRequests: Map<String, InputRequest>? = null,
                override val requestState: String? = null,
                override val ttlMs: TtlMs = TtlMs.of(0),
                override val cacheScope: CacheScope = CacheScope.public,
                override val _meta: Meta = Meta.default
            ) : HasMeta, HasInputRequired, CacheableResult
        }
    }

    object List {

        @JsonSerializable
        @PolymorphicLabel("resources/list")
        data class Request(override val params: Params? = null, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = of("resources/list")

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : PaginatedRequest, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val resources: kotlin.collections.List<McpResource>,
                override val nextCursor: Cursor? = null,
                override val ttlMs: TtlMs = TtlMs.of(0),
                override val cacheScope: CacheScope = CacheScope.public,
                override val _meta: Meta = Meta.default
            ) : PaginatedResponse, HasMeta, CacheableResult
        }

        data object Changed {

            @JsonSerializable
            @PolymorphicLabel("notifications/resources/list_changed")
            data class Notification(override val params: Params? = null, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
                override val method = of("notifications/resources/list_changed")

                @JsonSerializable
                data class Params(override val _meta: Meta = Meta.default) : HasMeta
            }
        }
    }

    object ListTemplates {

        @JsonSerializable
        @PolymorphicLabel("resources/templates/list")
        data class Request(override val params: Params? = null, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = of("resources/templates/list")

            @JsonSerializable
            data class Params(
                override val cursor: Cursor? = null,
                override val _meta: Meta = Meta.default
            ) : PaginatedRequest, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse {
            @JsonSerializable
            data class Result(
                val resourceTemplates: kotlin.collections.List<McpResource>,
                override val nextCursor: Cursor? = null,
                override val ttlMs: TtlMs = TtlMs.of(0),
                override val cacheScope: CacheScope = CacheScope.public,
                override val _meta: Meta = Meta.default
            ) : PaginatedResponse, HasMeta, CacheableResult
        }
    }

    data object Updated {

        @JsonSerializable
        @PolymorphicLabel("notifications/resources/updated")
        data class Notification(override val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = of("notifications/resources/updated")

            @JsonSerializable
            data class Params(val uri: McpUri, override val _meta: Meta = Meta.default) : HasMeta
        }
    }
}
