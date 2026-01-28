package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.CompletionContext
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable

object McpCompletion : McpRpc {
    override val Method = McpRpcMethod.of("completion/complete")

    @JsonSerializable
    data class Request(
        val ref: Reference,
        val argument: CompletionArgument,
        val context: CompletionContext = CompletionContext(),
        override val _meta: Meta = Meta.default
    ) : ClientMessage.Request, HasMeta

    @JsonSerializable
    data class Response(
        val completion: Completion,
        override val _meta: Meta = Meta.default
    ) : ServerMessage.Response,
        HasMeta
}
