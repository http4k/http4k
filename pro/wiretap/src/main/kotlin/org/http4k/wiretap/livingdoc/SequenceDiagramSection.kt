/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.otel.toMermaid
import org.http4k.wiretap.otel.toSequenceDiagram

object SequenceDiagramSection : LivingDocSection {
    override fun render(detail: TraceDetail, transactions: List<WiretapTransaction>): String {
        val diagram = detail.toSequenceDiagram()
        if (diagram.messages.isEmpty()) return ""

        return buildString {
            appendLine()
            appendLine("```mermaid")
            appendLine(diagram.toMermaid())
            appendLine("```")
        }
    }
}
