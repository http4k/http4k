package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpPing : McpRpc {
    override val Method = of("ping")

    @JsonSerializable
    data object Request : ClientMessage.Request, ServerMessage.Notification
}
