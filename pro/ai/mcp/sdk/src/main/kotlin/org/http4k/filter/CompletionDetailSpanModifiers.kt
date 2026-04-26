/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.util.McpJson

/**
 * Opt-in span modifiers that capture completion arguments and result values.
 * May contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 * Note: gen_ai.completion.* attributes are http4k custom conventions, not official OTel semantic conventions.
 */
object CompletionDetailSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, request: McpRequest) {
        if (request.message is McpCompletion.Request) {
            sb.setAttribute("gen_ai.completion.arguments", McpJson.asFormatString(request.message.params.argument))
        }
    }

    override operator fun invoke(sb: Span, response: McpResponse) {
        if (response is McpResponse.Ok && response.message is McpCompletion.Response) {
            sb.setAttribute("gen_ai.completion.result", McpJson.asFormatString(response.message.result.completion))
        }
    }
}
