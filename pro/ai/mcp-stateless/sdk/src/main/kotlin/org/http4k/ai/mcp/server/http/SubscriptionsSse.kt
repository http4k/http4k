/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.accepted
import org.http4k.lens.ALLOW
import org.http4k.lens.Header
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseResponse

// The SSE face of the stateless transport: a `subscriptions/listen` POST that Accepts text/event-stream
// opens a long-lived stream; anything else on this face is 405 (normal POSTs fall through to the http face).
fun SubscriptionsSse(protocol: McpProtocol, path: String = "/mcp") =
    path bind sse(TEXT_EVENT_STREAM.accepted() bind { req: Request ->
        when (req.method) {
            POST -> protocol.listen(req)
            else -> SseResponse(METHOD_NOT_ALLOWED, listOf(Header.ALLOW.meta.name to POST.name)) { it.close() }
        }
    })
