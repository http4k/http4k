/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

/**
 * Opt-in span modifiers that capture prompt arguments and result messages.
 * May contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 * Note: gen_ai.prompt.* attributes are http4k custom conventions, not official OTel semantic conventions.
 */
object GetPromptDetailSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpPrompt.Get.Method

    override fun request(sb: Span, request: McpNodeType) {
        McpJson.fields(request).toMap()["arguments"]?.let {
            sb.setAttribute("gen_ai.prompt.arguments", McpJson.compact(it))
        }
    }

    override fun response(sb: Span, response: McpNodeType) {
        val result = McpJson.fields(response).toMap()["result"] ?: return
        McpJson.fields(result).toMap()["messages"]?.let {
            sb.setAttribute("gen_ai.prompt.result", McpJson.compact(it))
        }
    }
}
