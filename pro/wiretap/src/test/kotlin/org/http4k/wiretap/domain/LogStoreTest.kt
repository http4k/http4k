/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.testing.logs.TestLogRecordData
import org.junit.jupiter.api.Test

class LogStoreTest {

    private val store = LogStore.InMemory(maxLogs = 3)

    private fun logRecord(
        traceId: String = "00000000000000000000000000000001",
        spanId: String = "0000000000000001",
        body: String = "TestEvent",
        severity: Severity = Severity.INFO,
        timestampNanos: Long = 1000000L
    ) = TestLogRecordData.builder()
        .setSpanContext(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
        .setBody(body)
        .setSeverity(severity)
        .setTimestamp(timestampNanos, java.util.concurrent.TimeUnit.NANOSECONDS)
        .setAttributes(Attributes.of(AttributeKey.stringKey("key"), "value"))
        .build()

    @Test
    fun `records and retrieves logs newest first`() {
        store.record(logRecord(body = "first", timestampNanos = 1000000L))
        store.record(logRecord(body = "second", timestampNanos = 2000000L))

        val logs = store.all()
        assertThat(logs, hasSize(equalTo(2)))
        assertThat(logs[0].bodyValue?.asString(), equalTo("second"))
        assertThat(logs[1].bodyValue?.asString(), equalTo("first"))
    }

    @Test
    fun `evicts oldest logs when exceeding max`() {
        store.record(logRecord(body = "first"))
        store.record(logRecord(body = "second"))
        store.record(logRecord(body = "third"))
        store.record(logRecord(body = "fourth"))

        val logs = store.all()
        assertThat(logs, hasSize(equalTo(3)))
        assertThat(logs[0].body.asString(), equalTo("fourth"))
    }

    @Test
    fun `retrieves logs for a specific trace`() {
        val traceA = "00000000000000000000000000000001"
        val traceB = "00000000000000000000000000000002"

        store.record(logRecord(traceId = traceA, body = "log-a"))
        store.record(logRecord(traceId = traceB, body = "log-b"))
        store.record(logRecord(traceId = traceA, body = "log-a2"))

        val logs = store.forTrace(traceA)
        assertThat(logs, hasSize(equalTo(2)))
        assertThat(logs.all { it.spanContext.traceId == traceA }, equalTo(true))
    }

    @Test
    fun `forTrace returns empty list for unknown trace`() {
        assertThat(store.forTrace("0000000000000000000000000000ffff"), hasSize(equalTo(0)))
    }
}
