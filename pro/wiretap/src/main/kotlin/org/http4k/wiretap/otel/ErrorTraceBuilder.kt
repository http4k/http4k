/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.wiretap.domain.OtelSpanId
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.isError

private val ROOT_PARENT_SPAN_ID = OtelSpanId.of("0000000000000000")

fun TraceDetail.toErrorTrace(): String {
    val spanById = spans.associateBy { it.spanId }
    val errorSpans = spans.filter { it.isError() }
    if (errorSpans.isEmpty()) return ""

    val errorPathSpanIds = mutableSetOf<OtelSpanId>()
    for (errorSpan in errorSpans) {
        var current = errorSpan
        errorPathSpanIds.add(current.spanId)
        while (current.parentSpanId != ROOT_PARENT_SPAN_ID && current.parentSpanId in spanById) {
            current = spanById.getValue(current.parentSpanId)
            errorPathSpanIds.add(current.spanId)
        }
    }

    val filtered = copy(spans = spans.filter { it.spanId in errorPathSpanIds })
    val diagram = filtered.toSequenceDiagram()
    return if (diagram.messages.isNotEmpty()) diagram.toMermaid() else ""
}
