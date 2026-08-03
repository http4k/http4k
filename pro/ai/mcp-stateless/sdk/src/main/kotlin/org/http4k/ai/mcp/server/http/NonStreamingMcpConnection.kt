/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.core.HttpFilter
import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

fun NonStreamingMcpConnection(
    mcpProtocol: McpProtocol,
    security: McpSecurity,
    path: String = "/mcp"
): RoutingHttpHandler = ServerFilters.CatchLensFailure().then(
    routes(
        security.routes + HttpFilter(security).then(
            path bind Method.POST to mcpProtocol
        )
    )
)
