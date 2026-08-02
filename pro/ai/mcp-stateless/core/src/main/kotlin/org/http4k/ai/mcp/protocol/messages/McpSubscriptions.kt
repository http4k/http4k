/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ResultType
import org.http4k.ai.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

/**
 * The opt-in subscription filter carried on a `subscriptions/listen` request (and echoed as the
 * honored subset on the acknowledgement). All fields are opt-in; absent = not requested.
 */
@JsonSerializable
data class SubscriptionFilter(
    val toolsListChanged: Boolean? = null,
    val promptsListChanged: Boolean? = null,
    val resourcesListChanged: Boolean? = null,
    val resourceSubscriptions: List<String>? = null
)

object McpSubscriptions {

    object Listen {

        @JsonSerializable
        @PolymorphicLabel("subscriptions/listen")
        data class Request(override val params: Params = Params(), override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = McpRpcMethod.of("subscriptions/listen")

            @JsonSerializable
            data class Params(
                val notifications: SubscriptionFilter = SubscriptionFilter(),
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }

        @JsonSerializable
        data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
            @JsonSerializable
            data class Result(
                val resultType: ResultType = ResultType.complete,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }
    }

    object Acknowledged {

        @JsonSerializable
        @PolymorphicLabel("notifications/subscriptions/acknowledged")
        data class Notification(override val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = McpRpcMethod.of("notifications/subscriptions/acknowledged")

            @JsonSerializable
            data class Params(
                val notifications: SubscriptionFilter,
                override val _meta: Meta = Meta.default
            ) : HasMeta
        }
    }
}
