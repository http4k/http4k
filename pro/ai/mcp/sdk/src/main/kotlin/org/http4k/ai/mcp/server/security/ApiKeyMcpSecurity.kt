/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.security

import org.http4k.core.Request
import org.http4k.filter.ServerFilters
import org.http4k.lens.Lens
import org.http4k.routing.RoutingHttpHandler

/**
 * API Key authentication for MCP servers.
 */
class ApiKeyMcpSecurity<T>(
    lens: (Lens<Request, T>),
    validate: (T) -> Boolean
) : McpSecurity {
    override val name = "Api Key"
    override val routes = emptyList<RoutingHttpHandler>()
    override val filter = ServerFilters.ApiKeyAuth(lens, validate)
}
