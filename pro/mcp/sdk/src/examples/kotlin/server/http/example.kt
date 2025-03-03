package server.http

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import org.http4k.server.Helidon
import org.http4k.server.asServer
import server.completions
import server.prompts
import server.resources
import server.sampling
import server.tools

/**
 * This example demonstrates how to create an MCP server using the HTTP-only protocol.
 */
fun main() {
    val mcpServer = mcpHttp(
        McpEntity.of("http4k mcp via HTTP"), Version.of("0.1.0"),
        prompts(),
        resources(),
        tools(),
        sampling(),
        completions()
    )

    mcpServer.asServer(Helidon(3001)).start()
}

