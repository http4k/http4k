/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.wiretap.domain.OtelSpanId
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.SpanDetail

fun testSpanDetail(
    spanId: String,
    parentSpanId: String,
    name: String,
    kind: String,
    serviceName: String,
    statusCode: String = "OK",
    durationMs: Long = 10L,
    attributes: List<SpanAttribute> = emptyList()
) = SpanDetail(
    spanId = OtelSpanId.of(spanId),
    parentSpanId = OtelSpanId.of(parentSpanId),
    name = name,
    kind = kind,
    durationMs = durationMs,
    statusCode = statusCode,
    statusDescription = "",
    serviceName = serviceName,
    depth = 0,
    startOffsetPercent = 0.0,
    widthPercent = 100.0,
    attributes = attributes,
    baggageAttributes = emptyList(),
    resourceAttributes = emptyList(),
    events = emptyList(),
    links = emptyList()
)
