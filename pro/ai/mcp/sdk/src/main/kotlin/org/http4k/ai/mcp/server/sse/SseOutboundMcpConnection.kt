/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.sse

import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.SessionState.Invalid
import org.http4k.ai.mcp.server.protocol.SessionState.Valid
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse

/**
 * Persistent SSE which routes outbound MCP messages for the allocated session to the client
 */
fun SseOutboundMcpConnection(protocol: McpProtocol<Sse>) =
    "/sse" bind { req: Request ->
        when (val sessionState = protocol.retrieveSession(req)) {
            is Valid
 -> SseResponse(OK) {
                protocol.subscribe(Subscription(sessionState.session), it, req)
                it.send(
                    SseMessage.Event(
                        "endpoint",
                        Request(GET, "/message").with(sessionId of sessionState.session.id).uri.toString()
                    )
                )
            }

            Invalid -> SseResponse(NOT_FOUND) { it.close() }
        }
    }
