/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpPrompt

object GetPromptSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpJsonRpcRequest) {
        if (request is McpPrompt.Get.Request) {
            sb.setAttribute("gen_ai.operation.name", "get_prompt")
            sb.setAttribute("gen_ai.prompt.name", request.params.name.value)
        }
    }
}
