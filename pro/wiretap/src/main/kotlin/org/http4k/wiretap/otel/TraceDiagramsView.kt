/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.TraceDetail

data class TraceDiagramsView(
    val sequenceDiagram: String,
    val interactionDiagram: String,
    val timingEntries: List<TimingEntry>,
    val errorTrace: String,
    val criticalPath: String
) : ViewModel

fun TraceDetail.toTraceDiagramsView(): TraceDiagramsView {
    val sequence = toSequenceDiagram()
    return TraceDiagramsView(
        sequenceDiagram = if (sequence.messages.isNotEmpty()) sequence.toMermaid() else "",
        interactionDiagram = toInteractionDiagram(),
        timingEntries = toTimingTable(),
        errorTrace = toErrorTrace(),
        criticalPath = toCriticalPath()
    )
}
