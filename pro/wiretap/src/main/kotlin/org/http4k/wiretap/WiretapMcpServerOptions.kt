/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.ai.mcp.server.security.NoMcpSecurity

/**
 * Options for configuring the Wiretap MCP server. Defaults to NoMcpSecurity and "/mcp" path
 */
data class WiretapMcpServerOptions(val security: McpSecurity, val path: String) {
    companion object {
        val default = WiretapMcpServerOptions(NoMcpSecurity, "/mcp")
    }
}
