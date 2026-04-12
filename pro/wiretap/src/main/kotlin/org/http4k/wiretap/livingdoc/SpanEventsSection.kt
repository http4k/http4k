/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.WiretapTransaction

object SpanEventsSection : LivingDocSection {
    override fun render(detail: TraceDetail, transactions: List<WiretapTransaction>): String {
        val spansWithEvents = detail.spans.filter { it.events.isNotEmpty() }
        if (spansWithEvents.isEmpty()) return ""

        return buildString {
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
        }
    }
}
