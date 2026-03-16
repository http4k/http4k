/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.testing.logs.TestLogRecordData
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.OtelTraceId
import org.junit.jupiter.api.Test

class WiretapLogRecordExporterTest {

    private val logStore = LogStore.InMemory()
    private val exporter = WiretapLogRecordExporter(logStore)

    @Test
    fun `exports log records to LogStore`() {
        val log = TestLogRecordData.builder()
            .setSpanContext(SpanContext.create("00000000000000000000000000000001", "0000000000000001", TraceFlags.getSampled(), TraceState.getDefault()))
            .setBody("TestEvent")
            .setSeverity(Severity.INFO)
            .build()

        val result = exporter.export(listOf(log))

        assertThat(result.isSuccess, equalTo(true))
        assertThat(logStore.forTrace(OtelTraceId.of("00000000000000000000000000000001")), hasSize(equalTo(1)))
    }

    @Test
    fun `flush returns success`() {
        assertThat(exporter.flush().isSuccess, equalTo(true))
    }

    @Test
    fun `shutdown returns success`() {
        assertThat(exporter.shutdown().isSuccess, equalTo(true))
    }
}
