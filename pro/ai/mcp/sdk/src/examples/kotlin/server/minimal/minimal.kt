package server.minimal

import org.http4k.filter.debugMcp
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import java.time.Instant

/**
 * This example demonstrates how to create an minimal MCP tool server using the SSE-only protocol.
 */
fun main() {
    mcpHttpStreaming(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        NoMcpSecurity,
        Tool("time", "get the time") bind {
            ToolResponse.Ok(listOf(Content.Text(Instant.now().toString())))
        }
    ).debugMcp().asServer(JettyLoom(4001)).start()
}

