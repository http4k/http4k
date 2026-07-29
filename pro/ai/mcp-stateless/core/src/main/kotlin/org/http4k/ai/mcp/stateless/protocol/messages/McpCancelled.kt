/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.McpMessageId
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpCancelled {

    @JsonSerializable
    @PolymorphicLabel("notifications/cancelled")
    data class Notification(override val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
        override val method = McpRpcMethod.of("notifications/cancelled")

        @JsonSerializable
        data class Params(
            val requestId: McpMessageId,
            val reason: String? = null,
            override val _meta: Meta = Meta.default,
        ) : HasMeta
    }
}
