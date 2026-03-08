/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable

object McpCancelled : McpRpc {
    override val Method = McpRpcMethod.of("notifications/cancelled")

    @JsonSerializable
    data class Notification(
        val requestId: McpMessageId,
        val reason: String? = null,
        override val _meta: Meta = Meta.default,
    ) : ClientMessage.Notification, ServerMessage.Notification, HasMeta
}

