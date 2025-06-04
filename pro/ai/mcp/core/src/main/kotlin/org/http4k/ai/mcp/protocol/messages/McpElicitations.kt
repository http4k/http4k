package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable

object McpElicitations : McpRpc {
    override val Method = McpRpcMethod.of("elicitation/create")

    @JsonSerializable
    data class Request(
        val message: String,
        val requestedSchema: McpNodeType,
        override val _meta: Meta = Meta.default
    ) : ServerMessage.Request, ClientMessage.Request, HasMeta

    @JsonSerializable
    data class Response(
        val action: ElicitationAction,
        val content: McpNodeType,
        override val _meta: Meta = Meta.default
    ) : ServerMessage.Response, ClientMessage.Response, HasMeta
}
