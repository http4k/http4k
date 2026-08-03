/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.accepted
import org.http4k.lens.ALLOW
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse
import org.http4k.sse.then

fun StreamingMcpConnection(mcpProtocol: McpProtocol, security: McpSecurity, path: String = "/mcp"): RoutingSseHandler =
    SseFilter(security).then(
    path bind sse(TEXT_EVENT_STREAM.accepted() bind { req: Request ->
        when (req.method) {
            POST -> when (Header.MCP_METHOD(req)) {
                McpRpcMethod.of("subscriptions/listen") -> mcpProtocol.listen(req)
                else -> SseResponse(OK, emptyList(), handled = false) { it.close() }
            }
            else -> SseResponse(METHOD_NOT_ALLOWED, listOf(Header.ALLOW.meta.name to POST.name)) { it.close() }
        }
    })
)
