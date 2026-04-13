package org.http4k.wiretap.otel.breakdown

import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.otel.toCriticalPath

object CriticalPathTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toCriticalPath().takeIf { it.isNotEmpty() }
            ?.let { Tab("critical-path", "Critical Path", renderer(MermaidDiagramView(it))) }
}
