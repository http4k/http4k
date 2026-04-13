package org.http4k.wiretap.otel.breakdown

import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.otel.toTimingTable

object TimingTab : TabContentRenderer {
    override fun render(detail: TraceDetail, renderer: TemplateRenderer) =
        detail.toTimingTable().takeIf { it.isNotEmpty() }
            ?.let { Tab("timing", "Timing", renderer(TimingTableView(it))) }
}
