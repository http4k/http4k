/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskMeta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

object McpElicitations : McpRpc {
    override val Method = McpRpcMethod.of("elicitation/create")

    @JsonSerializable
    @PolymorphicLabel("elicitation/create")
    data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
        @JsonSerializable
        @Polymorphic("mode")
        sealed class Params : ServerMessage.Request, HasMeta {
            @JsonSerializable
            @PolymorphicLabel("form")
            data class Form(
                val message: String,
                val requestedSchema: McpNodeType,
                override val _meta: Meta = Meta.default,
                val task: TaskMeta? = null
            ) : Params()

            @JsonSerializable
            @PolymorphicLabel("url")
            data class Url(
                val message: String,
                val url: Uri,
                val elicitationId: ElicitationId,
                override val _meta: Meta = Meta.default,
                val task: TaskMeta? = null
            ) : Params()
        }
    }

    @JsonSerializable
    data class Response(val result: Result, override val id: McpNodeType?) : McpJsonRpcResonse {
        @JsonSerializable
        data class Result(
            val action: ElicitationAction? = null,
            val content: McpNodeType? = null,
            val task: Task? = null,
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Response, HasMeta
    }

    object Complete : McpRpc {
        override val Method = McpRpcMethod.of("notifications/elicitation/complete")

        @JsonSerializable
        @PolymorphicLabel("notifications/elicitation/complete")
        data class Notification(val params: Params, override val id: McpNodeType? = null) : McpJsonRpcRequest() {
            @JsonSerializable
            data class Params(
                val elicitationId: ElicitationId,
                override val _meta: Meta = Meta.default
            ) : ServerMessage.Notification
        }
    }
}
