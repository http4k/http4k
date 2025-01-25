package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import se.ansman.kotshi.JsonSerializable

object McpPing : HasMethod {
    override val Method = of("ping")

    @JsonSerializable
    data object Request : ClientMessage.Request
}
