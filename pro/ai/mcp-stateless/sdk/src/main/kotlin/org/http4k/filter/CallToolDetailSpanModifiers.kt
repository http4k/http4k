/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.util.McpJson

/**
 * Opt-in span modifiers that capture tool call arguments and results.
 * These may contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 */
object CallToolDetailSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpRequest) {
        if (request.message is McpTool.Call.Request) {
            sb.setAttribute("gen_ai.tool.call.arguments", McpJson.asFormatString(request.message.params.arguments))
        }
    }

    override operator fun invoke(sb: Span, response: McpResponse) {
        if (response is McpResponse.Ok && response.message is McpTool.Call.Response) {
            response.message.result.content?.let {
                sb.setAttribute("gen_ai.tool.call.result", McpJson.asFormatString(it))
            }
        }
    }
}
