/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
