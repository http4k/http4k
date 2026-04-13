/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.util.MarkdownContent

object SpanEventsSection : LivingDocSection {
    override fun renderMarkdown(detail: TraceDetail, transactions: List<WiretapTransaction>): MarkdownContent {
        val spansWithEvents = detail.spans.filter { it.events.isNotEmpty() }
        if (spansWithEvents.isEmpty()) return MarkdownContent.empty

        return MarkdownContent.of(buildString {
            appendLine()
            appendLine("### Events")
            spansWithEvents.forEach { span ->
                span.events.forEach { event ->
                    appendLine()
                    appendLine("#### ${event.name} on `${span.name}` (${span.serviceName})")
                    event.attributes.forEach { attr ->
                        appendLine("- **${attr.key}**: ${attr.value}")
                    }
                }
            }
        })
    }
}
