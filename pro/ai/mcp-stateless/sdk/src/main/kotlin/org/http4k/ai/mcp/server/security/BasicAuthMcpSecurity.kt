/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.security

import org.http4k.core.Credentials
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler

/**
 * Basic authentication for MCP servers.
 */
class BasicAuthMcpSecurity(realm: String, credentials: (Credentials) -> Boolean) : McpSecurity {
    override val name = "Basic Auth"
    override val routes = emptyList<RoutingHttpHandler>()
    override val filter = ServerFilters.BasicAuth(realm, credentials)
}
