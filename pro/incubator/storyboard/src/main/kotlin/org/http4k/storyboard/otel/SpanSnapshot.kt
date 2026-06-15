/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.otel

import io.opentelemetry.sdk.trace.data.SpanData

data class SpanSnapshot(
    val name: String,
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
    val kind: String,
    val startEpochNanos: Long,
    val endEpochNanos: Long,
    val attributes: Map<String, String>,
    val events: List<Event>,
    val statusCode: String
) {
    data class Event(
        val name: String,
        val epochNanos: Long,
        val attributes: Map<String, String>
    )
}

internal fun SpanData.toSnapshot(): SpanSnapshot = SpanSnapshot(
    name = name,
    traceId = traceId,
    spanId = spanId,
    parentSpanId = parentSpanContext.spanId.takeUnless { it == "0000000000000000" },
    kind = kind.name,
    startEpochNanos = startEpochNanos,
    endEpochNanos = endEpochNanos,
    attributes = attributes.asMap().entries.associate { (k, v) -> k.key to v.toString() }.toSortedMap(),
    events = events.map { e ->
        SpanSnapshot.Event(
            name = e.name,
            epochNanos = e.epochNanos,
            attributes = e.attributes.asMap().entries.associate { (k, v) -> k.key to v.toString() }.toSortedMap()
        )
    },
    statusCode = status.statusCode.name
)
