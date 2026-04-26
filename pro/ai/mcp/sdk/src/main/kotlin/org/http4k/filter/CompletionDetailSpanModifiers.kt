/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

/**
 * Opt-in span modifiers that capture completion arguments and result values.
 * May contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 * Note: gen_ai.completion.* attributes are http4k custom conventions, not official OTel semantic conventions.
 */
object CompletionDetailSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpCompletion.Method

    override fun request(sb: Span, request: McpNodeType) {
        McpJson.fields(request).toMap()["argument"]?.let {
            sb.setAttribute("gen_ai.completion.arguments", McpJson.compact(it))
        }
    }

    override fun response(sb: Span, response: McpNodeType) {
        val result = McpJson.fields(response).toMap()["result"] ?: return
        McpJson.fields(result).toMap()["completion"]?.let {
            sb.setAttribute("gen_ai.completion.result", McpJson.compact(it))
        }
    }
}
