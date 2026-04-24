/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcResponse
import org.http4k.ai.mcp.protocol.messages.McpTool

object CallToolSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpJsonRpcRequest) {
        if (request is McpTool.Call.Request) {
            sb.setAttribute("gen_ai.operation.name", "execute_tool")
            sb.setAttribute("gen_ai.tool.name", request.params.name.value)
        }
    }

    override operator fun invoke(sb: Span, response: McpJsonRpcResponse) {
        if (response is McpTool.Call.Response) {
            response.result.isError?.takeIf { it }?.let {
                sb.setStatus(StatusCode.ERROR)
                sb.setAttribute("error.type", "tool_error")
            }
        }
    }
}
