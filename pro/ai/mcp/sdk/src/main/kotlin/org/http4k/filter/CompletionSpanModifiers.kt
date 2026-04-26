/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

object CompletionSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpCompletion.Method

    override fun request(sb: Span, request: McpNodeType) {
        sb.setAttribute("gen_ai.operation.name", "complete")
        val ref = McpJson.fields(request).toMap()["ref"] ?: return
        val refFields = McpJson.fields(ref).toMap()
        val refLabel = refFields["name"]?.let { McpJson.text(it) }
            ?: refFields["uri"]?.let { McpJson.text(it) }
        if (refLabel != null) sb.setAttribute("mcp.completion.ref", refLabel)
    }
}
