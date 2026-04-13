/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.TraceDetail

data class Tab(val id: String, val name: String, val content: String)

fun interface TabContentRenderer {
    fun render(renderer: TemplateRenderer): Tab?
}

data class TraceBreakdownView(val tabs: List<Tab>) : ViewModel

fun TraceDetail.toTraceBreakdownView(renderer: TemplateRenderer, extraTabs: List<TabContentRenderer> = emptyList()) =
    TraceBreakdownView(
        tabs = listOfNotNull(
            toSequenceDiagram().toMermaid().takeIf { it.isNotEmpty() }
                ?.let { Tab("sequence", "Sequence", renderer(MermaidDiagramView(it))) },
            toInteractionDiagram().takeIf { it.isNotEmpty() }
                ?.let { Tab("interaction", "Interactions", renderer(MermaidDiagramView(it))) },
            toTimingTable().takeIf { it.isNotEmpty() }
                ?.let { Tab("timing", "Timing", renderer(TimingTableView(it))) },
            toErrorTrace().takeIf { it.isNotEmpty() }
                ?.let { Tab("error", "Errors", renderer(MermaidDiagramView(it))) },
            toCriticalPath().takeIf { it.isNotEmpty() }
                ?.let { Tab("critical-path", "Critical Path", renderer(MermaidDiagramView(it))) },
        ) + extraTabs.mapNotNull { it.render(renderer) }
    )
