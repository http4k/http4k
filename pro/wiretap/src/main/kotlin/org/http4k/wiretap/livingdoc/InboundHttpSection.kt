/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.util.MarkdownContent

object InboundHttpSection : LivingDocSection {
    override fun renderMarkdown(detail: TraceDetail, transactions: List<WiretapTransaction>): MarkdownContent {
        val inbound = transactions.filter { it.direction == Direction.Inbound }
        if (inbound.isEmpty()) return MarkdownContent.empty

        return MarkdownContent.of(buildString {
            appendLine()
            appendLine("### Inbound")
            inbound.forEach { append(renderTransaction(it)) }
        })
    }
}
