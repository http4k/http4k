/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.isError

data class TimingEntry(
    val serviceName: String,
    val operation: String,
    val kind: String,
    val durationMs: Long,
    val percentOfTotal: Double,
    val error: Boolean
)

fun TraceDetail.toTimingTable(): List<TimingEntry> {
    if (spans.isEmpty() || totalDurationMs == 0L) return emptyList()

    return spans
        .map { span ->
            TimingEntry(
                serviceName = span.serviceName,
                operation = span.name,
                kind = span.kind,
                durationMs = span.durationMs,
                percentOfTotal = (span.durationMs.toDouble() / totalDurationMs * 100),
                error = span.isError()
            )
        }
        .sortedByDescending { it.durationMs }
}
