/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

object McpElicitation {

    @JsonSerializable
    data class Create(val params: Params, val method: McpRpcMethod = McpRpcMethod.of("elicitation/create")) {

        @JsonSerializable
        @Polymorphic("mode")
        sealed class Params {
            @JsonSerializable
            @PolymorphicLabel("form")
            data class Form(val message: String, val requestedSchema: McpNodeType) : Params()

            @JsonSerializable
            @PolymorphicLabel("url")
            data class Url(val message: String, val url: Uri) : Params()
        }
    }

    @JsonSerializable
    data class Result(val action: ElicitationAction, val content: McpNodeType? = null)
}
