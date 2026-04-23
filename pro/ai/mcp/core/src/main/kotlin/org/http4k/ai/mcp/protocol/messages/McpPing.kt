/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpPing : McpRpc {
    override val Method = of("ping")

    @JsonSerializable
    @PolymorphicLabel("ping")
    data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
        override val method = McpPing.Method

        @JsonSerializable
        data class Params(override val _meta: Meta = Meta.default) : ClientMessage.Request, ServerMessage.Request
    }
}
