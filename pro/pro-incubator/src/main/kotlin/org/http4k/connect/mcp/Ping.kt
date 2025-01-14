package org.http4k.connect.mcp

import org.http4k.connect.mcp.McpRpcMethod.Companion.of

object Ping : HasMethod {
    override val Method = of("ping")

    data object Request : ClientMessage.Request
}
