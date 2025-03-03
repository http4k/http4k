package server.ws

import org.http4k.filter.debug
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpWs
import org.http4k.server.Helidon
import org.http4k.server.asServer
import server.completions
import server.prompts
import server.resources
import server.sampling
import server.tools

/**
 * This example demonstrates how to create an MCP server using the standard WS protocol.
 */
fun main() {
    val mcpServer = mcpWs(
        ServerMetaData(
            McpEntity.of("http4k mcp via WS"), Version.of("0.1.0"),
            *ProtocolCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools(),
        sampling(),
        completions()
    )

    mcpServer.debug(debugStream = true).asServer(Helidon(3001)).start()
}
