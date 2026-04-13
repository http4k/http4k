package org.http4k.wiretap.otel.breakdown

import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.otel.toInteractionDiagram

object InteractionDiagramTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toInteractionDiagram().takeIf { it.isNotEmpty() }
            ?.let { Tab("interaction", "Interactions", renderer(MermaidDiagramView(it))) }
}
