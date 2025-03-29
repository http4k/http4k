package server.sse

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerProtocolCapability
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpSse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import server.completions
import server.prompts
import server.resources
import server.tools

/**
 * This example demonstrates how to create an MCP server using the standard SSE protocol.
 */
fun main() {
    val mcpServer = mcpSse(
        ServerMetaData(
            McpEntity.of("http4k mcp via SSE"), Version.of("0.1.0"),
            *ServerProtocolCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools(),
        completions()
    )

    mcpServer.asServer(Helidon(4001)).start()
}
