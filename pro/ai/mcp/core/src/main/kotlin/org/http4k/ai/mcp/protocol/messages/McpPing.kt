/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpPing : McpRpc {
    override val Method = of("ping")

    @JsonSerializable
    data class Request(override val _meta: Meta = Meta.default) : ClientMessage.Request, ServerMessage.Request
}
