package server.security

import org.http4k.core.Credentials
import org.http4k.filter.debug
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.BasicAuthMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * This example demonstrates how to secure an MCP server with basic auth built into the server.
 */
fun main() {
    val secureMcpServer = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        BasicAuthMcpSecurity("realm") { it == Credentials("foo", "bar") }
    )

    secureMcpServer
        .debug(debugStream = true)
        .asServer(JettyLoom(3001))
        .start()
}
