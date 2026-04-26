/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.util.McpNodeType

interface McpOpenTelemetrySpanModifiers {
    val method: McpRpcMethod
    fun request(sb: Span, request: McpNodeType) {}
    fun response(sb: Span, response: McpNodeType) {}
}
