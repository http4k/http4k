package org.http4k.connect.mcp

import org.http4k.connect.mcp.McpRpcMethod.Companion.of

object Ping : HasMethod {
    override val method = of("ping")

    data object Request : ClientRequest
}
