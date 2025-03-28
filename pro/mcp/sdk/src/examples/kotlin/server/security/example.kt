package server.security

import org.http4k.core.Credentials
import org.http4k.filter.debug
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttpStreaming
import org.http4k.security.BasicAuthSecurity
import org.http4k.security.then
import org.http4k.server.Helidon
import org.http4k.server.asServer

/**
 * This example demonstrates how to secure an MCP server with basic auth built into the server.
 */
fun main() {
    val baseMcpServer = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0"))
    )

    val secureMcpServer = BasicAuthSecurity("realm", Credentials("foo", "bar"))
        .then(baseMcpServer)
    secureMcpServer
        .debug(debugStream = true)
        .asServer(Helidon(3001))
        .start()
}
