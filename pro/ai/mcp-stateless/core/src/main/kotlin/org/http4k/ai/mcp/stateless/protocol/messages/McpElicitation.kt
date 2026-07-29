/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import org.http4k.ai.mcp.stateless.model.ElicitationAction
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod
import org.http4k.ai.mcp.stateless.util.McpJson.obj
import org.http4k.ai.mcp.stateless.util.McpNodeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

object McpElicitation {

    @JsonSerializable
    @PolymorphicLabel("elicitation/create")
    data class Create(
        val params: Params,
        val method: McpRpcMethod = McpRpcMethod.of("elicitation/create")
    ) : InputRequest() {

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
    data class Result(val action: ElicitationAction, val content: McpNodeType = obj())
}
