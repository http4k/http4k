package org.http4k.connect.mcp

import org.http4k.connect.mcp.McpRpcMethod.Companion.of

object McpPing : HasMethod {
    override val Method = of("ping")

    data object Request : ClientMessage.Request
}
