package server.http

import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.Version
import org.http4k.routing.bind
import org.http4k.routing.mcpJsonRpc
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.time.Instant

/**
 * This example demonstrates how to create an minimal MCP tool server using the HTTP-only protocol.
 */
fun main() {
    mcpJsonRpc(
        McpEntity.of("http4k mcp via HTTP"), Version.of("0.1.0"),
        Tool("time", "get the time") bind {
            ToolResponse.Ok(listOf(Content.Text(Instant.now().toString())))
        }
    ).asServer(Helidon(3001)).start()
}

