/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpLogging {
    object SetLevel : McpRpc {
        override val Method = McpRpcMethod.of("logging/setLevel")

        @JsonSerializable
        @PolymorphicLabel("logging/setLevel")
        data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = SetLevel.Method

            @JsonSerializable
            data class Params(
                val level: LogLevel,
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Request, HasMeta
        }
    }

    object LoggingMessage : McpRpc {
        override val Method = McpRpcMethod.of("notifications/message")

        @JsonSerializable
        @PolymorphicLabel("notifications/message")
        data class Notification(val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = LoggingMessage.Method

            @JsonSerializable
            data class Params(
                val data: McpNodeType,
                val level: LogLevel,
                val logger: String? = null,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Notification, HasMeta
        }
    }
}
