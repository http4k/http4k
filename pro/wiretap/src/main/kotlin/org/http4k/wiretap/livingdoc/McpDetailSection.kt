/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.util.formatBody

object McpDetailSection : LivingDocSection {
    override fun render(detail: TraceDetail, transactions: List<WiretapTransaction>): String {
        val mcpSpan = detail.spans.firstOrNull { span ->
            span.attributes.any { it.key == "mcp.method.name" }
        } ?: return ""

        val args = mcpSpan.attributes.firstOrNull {
            it.key in setOf("gen_ai.tool.call.arguments", "gen_ai.prompt.arguments", "gen_ai.completion.arguments")
        }
        val result = mcpSpan.attributes.firstOrNull {
            it.key in setOf(
                "gen_ai.tool.call.result",
                "gen_ai.prompt.result",
                "gen_ai.completion.result",
                "gen_ai.resource.result"
            )
        }

        if (args == null && result == null) return ""

        return buildString {
            if (args != null) {
                appendLine()
                appendLine("### Arguments")
                appendLine()
                appendLine("```json")
                appendLine(formatBody(args.value, "application/json"))
                appendLine("```")
            }
            if (result != null) {
                appendLine()
                appendLine("### Result")
                appendLine()
                appendLine("```json")
                appendLine(formatBody(result.value, "application/json"))
                appendLine("```")
            }
        }
    }
}
