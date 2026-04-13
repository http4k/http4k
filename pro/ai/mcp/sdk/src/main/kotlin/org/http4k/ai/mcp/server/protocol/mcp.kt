/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.jsonrpc.JsonRpcRequest

data class McpRequest(val session: Session, val json: JsonRpcRequest<McpNodeType>, val http: Request)

data class McpResponse(val json: McpNodeType)

typealias McpHandler = (McpRequest) -> McpResponse

fun interface McpFilter : (McpHandler) -> McpHandler {
    companion object
}

val McpFilter.Companion.NoOp: McpFilter get() = { it }

fun McpFilter.then(next: McpHandler) = this(next)

fun McpFilter.then(next: McpFilter) = McpFilter { this(next(it)) }

typealias MatchedHandler = (Client) -> McpHandler

interface RoutingMcpHandler {
    operator fun invoke(request: McpRequest): MatchedHandler?
}
