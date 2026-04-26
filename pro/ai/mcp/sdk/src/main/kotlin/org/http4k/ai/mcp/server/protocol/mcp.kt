/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.core.Request

data class McpRequest(val session: Session, val message: McpJsonRpcRequest, val http: Request)

sealed interface McpResponse {
    data class Ok(val message: McpJsonRpcMessage) : McpResponse
    data object Accepted : McpResponse
    data object Unknown : McpResponse
}

typealias McpHandler = (McpRequest) -> McpResponse

fun interface McpFilter : (McpHandler) -> McpHandler {
    companion object
}

val McpFilter.Companion.NoOp: McpFilter get() = { it }

fun McpFilter.then(next: McpHandler) = this(next)

fun McpFilter.then(next: McpFilter) = McpFilter { this(next(it)) }
