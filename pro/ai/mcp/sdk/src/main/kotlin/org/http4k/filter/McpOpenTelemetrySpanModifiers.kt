/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcResponse

interface McpOpenTelemetrySpanModifier {
    operator fun invoke(sb: Span, request: McpJsonRpcRequest) {}
    operator fun invoke(sb: Span, response: McpJsonRpcResponse) {}
}
