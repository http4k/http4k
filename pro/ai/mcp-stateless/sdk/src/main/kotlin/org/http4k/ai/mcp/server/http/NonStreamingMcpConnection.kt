/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.core.Method.POST
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind

fun NonStreamingMcpConnection(
    mcpProtocol: McpProtocol, path: String = "/mcp"
): RoutingHttpHandler = path bind POST to { req -> mcpProtocol(req) }
