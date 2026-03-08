/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.security

import org.http4k.routing.RoutingHttpHandler
import org.http4k.security.Security

/**
 * Provides a way to secure an MCP server using various authentication methods.
 */
interface McpSecurity : Security {
    val name: String
    val routes: List<RoutingHttpHandler>
}
