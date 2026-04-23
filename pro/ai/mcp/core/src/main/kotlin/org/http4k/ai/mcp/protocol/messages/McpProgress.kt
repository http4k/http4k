/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpProgress : McpRpc {
    override val Method = of("notifications/progress")

    @JsonSerializable
    @PolymorphicLabel("notifications/progress")
    data class Notification(val params: Params, override val id: McpNodeType? = null) : McpJsonRpcRequest() {
        @JsonSerializable
        data class Params(
            val progressToken: ProgressToken,
            val progress: Int,
            val total: Double?,
            val description: String?,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Notification, ClientMessage.Notification, HasMeta
    }
}
