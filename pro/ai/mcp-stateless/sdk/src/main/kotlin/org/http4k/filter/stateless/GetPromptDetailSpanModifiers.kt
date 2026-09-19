/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter.stateless

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.server.protocol.McpRequest

/**
 * Opt-in span modifiers that capture prompt arguments and result messages.
 * May contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 * Note: gen_ai.prompt.* attributes are http4k custom conventions, not official OTel semantic conventions.
 */
object GetPromptDetailSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpRequest) {
        if (request.message is McpPrompt.Get.Request) {
            request.message.params.arguments.forEach { (name, value) ->
                sb.setAttribute("gen_ai.prompt.variable.$name", value)
            }
        }
    }
}
