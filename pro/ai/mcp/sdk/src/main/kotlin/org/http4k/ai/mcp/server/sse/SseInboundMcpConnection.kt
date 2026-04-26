/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.sse

import org.http4k.ai.mcp.server.asHttp
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.SessionState.Invalid
import org.http4k.ai.mcp.server.protocol.SessionState.Valid
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.routing.bind
import org.http4k.sse.Sse

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing.
 */
fun SseInboundMcpConnection(protocol: McpProtocol<Sse>) =
    "/message" bind POST to { req ->
        when (val sessionState = protocol.retrieveSession(req)) {
            is Valid
 -> protocol.receive(protocol.transportFor(Subscription(sessionState.session)), sessionState, req).asHttp(ACCEPTED)
            Invalid -> Response(BAD_REQUEST)
        }
    }
