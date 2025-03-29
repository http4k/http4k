package server.stdio

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerProtocolCapability
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpStdIo
import server.prompts
import server.resources
import server.tools

/**
 * This example demonstrates how to create an MCP server using the standard STDIO protocol.
 */
fun main() {
    mcpStdIo(
        ServerMetaData(
            McpEntity.of("stdio mcp via STDIO"), Version.of("0.1.0"),
            *ServerProtocolCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools()
    )
}
