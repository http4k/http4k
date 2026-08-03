/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.core.HttpFilter
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.CorsAndRebindProtection
import org.http4k.filter.CorsPolicy
import org.http4k.filter.PolyFilters
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.sse.SseFilter
import org.http4k.sse.then

/**
 * Stateless Streamable-HTTP MCP as a PolyHandler:
 *  - http face: POST -> single JSON response; other methods -> 405.
 *  - sse face (Accept: text/event-stream): POST subscriptions/listen -> long-lived stream; any other method
 *    -> an SSE-framed request/response (progress/log notifications followed by the terminal result).
 * Both bind the same [path]; the Accept header routes a request to the sse face, else the http face.
 */
fun HttpMcp(
    mcpProtocol: McpProtocol,
    security: McpSecurity,
    path: String = "/mcp",
    corsPolicy: CorsPolicy? = null
): PolyHandler = PolyFilters.CatchAll()
    .then(corsPolicy?.let { PolyFilters.CorsAndRebindProtection(it) } ?: PolyFilter { it })
    .then(
        poly(
            SseFilter(security).then(StreamingMcpConnection(mcpProtocol, path)),
            CatchLensFailure().then(
                routes(
                    security.routes + HttpFilter(security).then(
                        NonStreamingMcpConnection(mcpProtocol, path)
                    )
                )
            )
        )
    )

