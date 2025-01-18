package org.http4k.connect.mcp.protocol

import org.http4k.connect.mcp.protocol.McpRpcMethod.Companion.of

object McpPing : HasMethod {
    override val Method = of("ping")

    data object Request : ClientMessage.Request
}
