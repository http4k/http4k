/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.EventData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData

fun testSpanData(
    traceId: String,
    spanId: String = "1234567890abcdef",
    name: String = "test",
    parentSpanId: String = "0000000000000000",
    kind: SpanKind = SpanKind.SERVER,
    serviceName: String = "",
    startNanos: Long = 1000000,
    endNanos: Long = 2000000,
    status: StatusData = StatusData.ok(),
    events: List<EventData> = emptyList(),
    attributes: Attributes = Attributes.empty()
): SpanData {
    val builder = TestSpanData.builder()
        .setSpanContext(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
        .setParentSpanContext(SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault()))
        .setName(name)
        .setKind(kind)
        .setStartEpochNanos(startNanos)
        .setEndEpochNanos(endNanos)
        .setHasEnded(true)
        .setStatus(status)
        .setAttributes(attributes)
        .setEvents(events)
        .setTotalRecordedEvents(events.size)

    if (serviceName.isNotEmpty()) {
        builder.setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)))
    }

    return builder.build()
}
