/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.CompletionContext
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpCompletion : McpRpc {
    override val Method = McpRpcMethod.of("completion/complete")

    @JsonSerializable
    @PolymorphicLabel("completion/complete")
    data class Request(val params: Params, override val id: McpNodeType?) : McpJsonRpcRequest() {
        @JsonSerializable
        data class Params(
            val ref: Reference,
            val argument: CompletionArgument,
            val context: CompletionContext = CompletionContext(),
            override val _meta: Meta = Meta.default
        ) : ClientMessage.Request, HasMeta
    }

    @JsonSerializable
    data class Response(val result: Result, override val id: McpNodeType?) : McpJsonRpcResponse {
        @JsonSerializable
        data class Result(
            val completion: Completion,
            override val _meta: Meta = Meta.default
        ) : ServerMessage.Response, HasMeta
    }
}
