package org.http4k.wiretap

import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.ai.mcp.server.security.NoMcpSecurity

/**
 * Options for connecting to the underlying MCP server. Defaults to NoMcpSecurity and "/mcp" path
 */
data class McpServerOptions(val security: McpSecurity, val path: String) {
    companion object {
        val default = McpServerOptions(NoMcpSecurity, "/mcp")
    }
}
