package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

object McpCompletion : McpRpc {
    override val Method = McpRpcMethod.of("completion/complete")

    @JsonSerializable
    data class Request(
        val ref: Reference,
        val argument: CompletionArgument,
        override val _meta: Meta = default
    ) : ClientMessage.Request, HasMeta

    @JsonSerializable
    data class Response(
        val completion: Completion,
        override val _meta: Meta = default
    ) : ServerMessage.Response,
        HasMeta
}
