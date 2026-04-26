/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

/**
 * Opt-in span modifiers that capture tool call arguments and results.
 * These may contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 */
object CallToolDetailSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpTool.Call.Method

    override fun request(sb: Span, request: McpNodeType) {
        McpJson.fields(request).toMap()["arguments"]?.let {
            sb.setAttribute("gen_ai.tool.call.arguments", McpJson.compact(it))
        }
    }

    override fun response(sb: Span, response: McpNodeType) {
        val result = McpJson.fields(response).toMap()["result"] ?: return
        McpJson.fields(result).toMap()["content"]?.let {
            sb.setAttribute("gen_ai.tool.call.result", McpJson.compact(it))
        }
    }
}
