/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Root
import org.http4k.ai.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpRoot {
    object List : McpRpc {
        override val Method = McpRpcMethod.of("roots/list")

        @JsonSerializable
        @PolymorphicLabel("roots/list")
        data class Request(val params: Params? = null, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = List.Method

            @JsonSerializable
            data class Params(override val _meta: Meta = Meta.default) : ServerMessage.Request, HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(val roots: kotlin.collections.List<Root>, override val _meta: Meta = Meta.default) :
                ClientMessage.Response, HasMeta
        }
    }

    data object Changed : McpRpc {
        override val Method = McpRpcMethod.of("notifications/roots/list_changed")

        @JsonSerializable
        @PolymorphicLabel("notifications/roots/list_changed")
        data class Notification(val params: Params? = null, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Changed.Method

            @JsonSerializable
            data class Params(override val _meta: Meta = Meta.default) : ClientMessage.Notification
        }
    }
}
