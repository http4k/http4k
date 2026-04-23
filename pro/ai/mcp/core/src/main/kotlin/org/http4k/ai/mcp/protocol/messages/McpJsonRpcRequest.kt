package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.util.McpNodeType
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic

@JsonSerializable
@Polymorphic("method")
sealed class McpJsonRpcRequest {
    abstract val id: McpNodeType?
}

interface McpJsonRpcResonse {
    val id: McpNodeType?
}
