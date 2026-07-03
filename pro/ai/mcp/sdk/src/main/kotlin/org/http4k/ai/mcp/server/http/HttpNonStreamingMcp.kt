/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.core.Filter
import org.http4k.core.HttpFilter
import org.http4k.core.NoOp
import org.http4k.core.then
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.routes
import org.http4k.sse.Sse

/**
 * MCP server setup for Non-streaming HTTP-based MCP Servers.
 */
fun HttpNonStreamingMcp(
    mcpProtocol: McpProtocol<Sse>,
    security: McpSecurity,
    path: String = "/mcp",
    corsPolicy: CorsPolicy? = null
) = CatchAll()
    .then(CatchLensFailure())
    .then(corsPolicy?.let { ServerFilters.Cors(it) } ?: Filter.NoOp)
    .then(routes(security.routes + HttpFilter(security).then(HttpNonStreamingMcpConnection(mcpProtocol, path))))
