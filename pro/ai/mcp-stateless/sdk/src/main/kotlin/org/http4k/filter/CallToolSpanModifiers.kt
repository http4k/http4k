/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse

object CallToolSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpRequest) {
        if (request.message is McpTool.Call.Request) {
            sb.setAttribute("gen_ai.operation.name", "execute_tool")
            sb.setAttribute("gen_ai.tool.name", request.message.params.name.value)
        }
    }

    override operator fun invoke(sb: Span, response: McpResponse) {
        if (response is McpResponse.Ok && response.message is McpTool.Call.Response) {
            response.message.result.isError?.takeIf { it }?.let {
                sb.setStatus(StatusCode.ERROR)
                sb.setAttribute("error.type", "tool_error")
            }
        }
    }
}
