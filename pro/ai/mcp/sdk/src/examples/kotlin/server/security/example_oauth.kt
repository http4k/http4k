package server.security

import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.OAuthMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * This example demonstrates how to secure an MCP server with OAuth security built into the server.
 *
 * The security is exposed as a OAuth Protected Resource Server which lists the auth servers. This is the simplest
 * version of the configuration - there are other constructors of OAuthMcpSecurity available for more complex cases.
 */
fun main() {
    val secureMcpServer = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        OAuthMcpSecurity(Uri.of("http://oauth-server")) { it == "123" }
    )

    secureMcpServer
        .debug(debugStream = true)
        .asServer(JettyLoom(3001))
        .start()
}
