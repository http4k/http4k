package org.http4k.wiretap.otel.breakdown

import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.otel.toMermaid
import org.http4k.wiretap.otel.toSequenceDiagram

object SequenceDiagramTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toSequenceDiagram().toMermaid().takeIf { it.isNotEmpty() }
            ?.let { Tab("sequence", "Sequence", renderer(MermaidDiagramView(it))) }
}
