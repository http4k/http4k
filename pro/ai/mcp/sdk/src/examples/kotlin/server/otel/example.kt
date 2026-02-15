package server.otel

import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.filter.McpFilters
import org.http4k.filter.PromptFilters
import org.http4k.filter.ResourceFilters
import org.http4k.filter.ToolFilters
import org.http4k.filter.debug
import org.http4k.lens.contentType
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * This example demonstrates how to create an MCP server using the draft HTTP Streaming protocol.
 */
fun main() {

    val mcpServer = HttpStreamingMcp(
        McpProtocol(
            ServerMetaData(
                McpEntity.of("http4k mcp via SSE"), Version.of("0.1.0"),
                *ServerProtocolCapability.entries.toTypedArray()
            ),
            HttpSessions().apply { start() },
            TracedPrompt(PromptFilters::OpenTelemetryTracing),
            TracedResource(ResourceFilters::OpenTelemetryTracing),
            TracedTool(ToolFilters::OpenTelemetryTracing),
            mcpFilter = McpFilters.OpenTelemetryTracing()
        ),
        NoMcpSecurity
    )

    mcpServer.asServer(JettyLoom(4001)).start()

    JavaHttpClient().debug()(
        Request(POST, "http://localhost:4001/mcp")
            .contentType(APPLICATION_JSON)
            .body("""{"jsonrpc":"2.0","method":"tools/list","params":{"_meta":{}},"id":1}""")
    )
}

