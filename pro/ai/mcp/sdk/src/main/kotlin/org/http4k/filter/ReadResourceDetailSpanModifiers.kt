/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

/**
 * Opt-in span modifiers that capture resource read result contents.
 * May contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 * Note: gen_ai.resource.* attributes are http4k custom conventions, not official OTel semantic conventions.
 */
object ReadResourceDetailSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpResource.Read.Method

    override fun response(sb: Span, response: McpNodeType) {
        val result = McpJson.fields(response).toMap()["result"] ?: return
        McpJson.fields(result).toMap()["contents"]?.let {
            sb.setAttribute("gen_ai.resource.result", McpJson.compact(it))
        }
    }
}
