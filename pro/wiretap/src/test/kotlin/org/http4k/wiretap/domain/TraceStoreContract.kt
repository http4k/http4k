/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.http4k.wiretap.domain.Ordering.Ascending
import org.http4k.wiretap.domain.Ordering.Descending
import org.junit.jupiter.api.Test

interface TraceStoreContract {

    val store: TraceStore

    fun span(traceId: String, spanId: String = "1234567890abcdef", name: String = "test"): SpanData =

        TestSpanData.builder()
            .setSpanContext(
                SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault())
            )
            .setName(name)
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(1000)
            .setEndEpochNanos(2000)
            .setHasEnded(true)
            .setStatus(StatusData.ok())
            .build()

    @Test
    fun `record and get returns spans for trace`() {
        val s = span("00000000000000000000000000000001")
        store.record(s)

        val result = store.get(OtelTraceId.of("00000000000000000000000000000001"))
        assertThat(result.size, equalTo(1))
        assertThat(result.first().name, equalTo("test"))
    }

    @Test
    fun `get returns empty for unknown trace`() {
        assertThat(store.get(OtelTraceId.of("00000000000000000000000000000099")), equalTo(emptyList()))
    }

    @Test
    fun `traces with OldestFirst returns chronological order`() {
        store.record(span("00000000000000000000000000000001", spanId = "1111111111111111", name = "first"))
        store.record(span("00000000000000000000000000000002", spanId = "2222222222222222", name = "second"))

        val traces = store.traces(Ascending)
        assertThat(
            traces.keys.toList(), equalTo(
                listOf(
                    OtelTraceId.of("00000000000000000000000000000001"),
                    OtelTraceId.of("00000000000000000000000000000002")
                )
            )
        )
    }

    @Test
    fun `traces default returns newest first`() {
        store.record(span("00000000000000000000000000000001", spanId = "1111111111111111", name = "first"))
        store.record(span("00000000000000000000000000000002", spanId = "2222222222222222", name = "second"))

        val traces = store.traces(Descending)
        assertThat(
            traces.keys.toList(), equalTo(
                listOf(
                    OtelTraceId.of("00000000000000000000000000000002"),
                    OtelTraceId.of("00000000000000000000000000000001")
                )
            )
        )
    }

    @Test
    fun `traces groups by trace id`() {
        store.record(span("00000000000000000000000000000001", spanId = "1111111111111111"))
        store.record(span("00000000000000000000000000000001", spanId = "2222222222222222"))
        store.record(span("00000000000000000000000000000002", spanId = "3333333333333333"))

        val traces = store.traces(Descending)
        assertThat(traces.size, equalTo(2))
        assertThat(traces[OtelTraceId.of("00000000000000000000000000000001")]?.size, equalTo(2))
        assertThat(traces[OtelTraceId.of("00000000000000000000000000000002")]?.size, equalTo(1))
    }
}
