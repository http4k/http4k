package server.security

import org.http4k.filter.debug
import org.http4k.lens.Header
import org.http4k.lens.uuid
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.security.ApiKeyMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import java.util.UUID

/**
 * This example demonstrates how to secure an MCP server with API key auth built into the server.
 */
fun main() {
    val secureMcpServer = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        ApiKeyMcpSecurity(Header.uuid().required("API_KEY")) { it == UUID(0, 0) }
    )

    secureMcpServer
        .debug(debugStream = true)
        .asServer(JettyLoom(3001))
        .start()
}
