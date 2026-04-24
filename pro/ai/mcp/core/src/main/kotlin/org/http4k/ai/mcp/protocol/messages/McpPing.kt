/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpPing : McpRpc {
    override val Method = of("ping")

    @JsonSerializable
    @PolymorphicLabel("ping")
    data class Request(val params: Params? = null, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
        override val method = McpPing.Method

        @JsonSerializable
        data class Params(override val _meta: Meta = Meta.default) : HasMeta
    }
}
