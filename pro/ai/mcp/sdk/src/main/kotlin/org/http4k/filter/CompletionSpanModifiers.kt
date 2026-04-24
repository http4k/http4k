/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest

object CompletionSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpJsonRpcRequest) {
        if (request is McpCompletion.Request) {
            sb.setAttribute("gen_ai.operation.name", "complete")
            val refLabel = when (val ref = request.params.ref) {
                is Reference.Prompt -> ref.name
                is Reference.ResourceTemplate -> ref.uri.toString()
            }
            sb.setAttribute("mcp.completion.ref", refLabel)
        }
    }
}
