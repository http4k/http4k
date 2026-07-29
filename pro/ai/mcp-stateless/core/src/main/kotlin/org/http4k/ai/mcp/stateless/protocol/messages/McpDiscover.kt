/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.CacheScope
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion
import org.http4k.ai.mcp.stateless.protocol.ServerCapabilities
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpDiscover {

    @JsonSerializable
    @PolymorphicLabel("server/discover")
    data class Request(override val params: Params = Params(), override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
        override val method = McpRpcMethod.of("server/discover")

        @JsonSerializable
        data class Params(override val _meta: Meta = Meta.default) : HasMeta
    }

    @JsonSerializable
    data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse {
        @JsonSerializable
        data class Result(
            val supportedVersions: List<ProtocolVersion>,
            val capabilities: ServerCapabilities = ServerCapabilities(),
            val instructions: String? = null,
            override val ttlMs: TtlMs = TtlMs.of(0),
            override val cacheScope: CacheScope = CacheScope.public,
            override val _meta: Meta = Meta.default,
        ) : HasMeta, CacheableResult
    }
}
