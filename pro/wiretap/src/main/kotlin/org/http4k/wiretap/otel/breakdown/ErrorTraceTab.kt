package org.http4k.wiretap.otel.breakdown

import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.otel.toErrorTrace

object ErrorTraceTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toErrorTrace().takeIf { it.isNotEmpty() }
            ?.let { Tab("error", "Errors", renderer(MermaidDiagramView(it))) }
}
