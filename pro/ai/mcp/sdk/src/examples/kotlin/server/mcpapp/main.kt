package server.mcpapp

import org.http4k.ai.mcp.model.extension.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.filter.debugMcp
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * Example MCP server with an MCP App (interactive UI).
 *
 * This demonstrates:
 * 1. Declaring the MCP Apps extension capability
 * 2. Serving an HTML UI as a resource
 * 3. A tool that displays the UI
 * 4. A tool that handles form submissions from the UI
 *
 * To test with Claude Desktop, add to your claude_desktop_config.json:
 * {
 *   "mcpServers": {
 *     "order-app": {
 *       "command": "curl",
 *       "args": ["-N", "http://localhost:3000/mcp"]
 *     }
 *   }
 * }
 */

fun main() {
    val server = mcpHttpStreaming(
        ServerMetaData("order-app", "1.0.0").withExtensions(McpApps),
        NoMcpSecurity,
        OrderFormUi.resource,
        showOrderFormTool(),
        submitOrderTool()
    ).debugMcp()

    server.asServer(JettyLoom(3000)).start()
    println("MCP App server running on http://localhost:3000")
}
