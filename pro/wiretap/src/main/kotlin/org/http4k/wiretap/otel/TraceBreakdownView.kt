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
    fun render(detail: TraceDetail, renderer: TemplateRenderer): Tab?
}

data class TraceBreakdownView(val tabs: List<Tab>) : ViewModel

object SequenceDiagramTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toSequenceDiagram().toMermaid().takeIf { it.isNotEmpty() }
            ?.let { Tab("sequence", "Sequence", renderer(MermaidDiagramView(it))) }
}

object InteractionDiagramTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toInteractionDiagram().takeIf { it.isNotEmpty() }
            ?.let { Tab("interaction", "Interactions", renderer(MermaidDiagramView(it))) }
}

object TimingTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toTimingTable().takeIf { it.isNotEmpty() }
            ?.let { Tab("timing", "Timing", renderer(TimingTableView(it))) }
}

object ErrorTraceTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toErrorTrace().takeIf { it.isNotEmpty() }
            ?.let { Tab("error", "Errors", renderer(MermaidDiagramView(it))) }
}

object CriticalPathTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toCriticalPath().takeIf { it.isNotEmpty() }
            ?.let { Tab("critical-path", "Critical Path", renderer(MermaidDiagramView(it))) }
}

val defaultTabs = listOf(SequenceDiagramTab, InteractionDiagramTab, TimingTab, ErrorTraceTab, CriticalPathTab)

fun TemplateRenderer.renderTraceBreakdownView(
    detail: TraceDetail,
    extraTabs: List<TabContentRenderer> = emptyList()
) =
    TraceBreakdownView(
        tabs = (defaultTabs + extraTabs).mapNotNull { it.render(detail, this) }
    )
