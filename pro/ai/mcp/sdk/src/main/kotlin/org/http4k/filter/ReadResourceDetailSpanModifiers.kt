/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.util.McpJson

/**
 * Opt-in span modifiers that capture resource read result contents.
 * May contain sensitive data — add to spanModifiers explicitly, not included in defaults.
 * Note: gen_ai.resource.* attributes are http4k custom conventions, not official OTel semantic conventions.
 */
object ReadResourceDetailSpanModifiers : McpOpenTelemetrySpanModifier {
    override operator fun invoke(sb: Span, response: McpResponse) {
        if (response is McpResponse.Ok && response.message is McpResource.Read.Response) {
            sb.setAttribute("gen_ai.resource.result", McpJson.asFormatString((response.message as McpResource.Read.Response).result.contents))
        }
    }
}
