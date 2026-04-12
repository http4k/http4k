/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import io.opentelemetry.api.trace.SpanKind
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.util.FixedClock
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.otel.testSpanData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(ApprovalTest::class)
class TestReportRendererTest {

    private val traceStore = TraceStore.InMemory()
    private val logStore = LogStore.InMemory()
    private val transactionStore = TransactionStore.InMemory()
    private val renderer = TestReportRenderer(traceStore, logStore, transactionStore, FixedClock)

    @Test
    fun `renders empty report`(approver: Approver) {
        approver.assertApproved(renderer("Empty.test"))
    }

    @Test
    fun `renders stdout and stderr tabs`(approver: Approver) {
        approver.assertApproved(renderer("Output.test", stdOut = "stdout line", stdErr = "stderr line"))
    }

    @Test
    fun `renders traffic tab with recorded transactions`(approver: Approver) {
        transactionStore.record(
            HttpTransaction(
                request = Request(Method.GET, "/api/test"),
                response = Response(OK).body("test response"),
                start = FixedClock.instant(),
                duration = Duration.ofMillis(42)
            ),
            Direction.Inbound
        )

        approver.assertApproved(renderer("Traffic.test"))
    }

    @Test
    fun `renders multiple traces into a single report`(approver: Approver) {
        recordSpan("00000000000000000000000000000001", "aaaaaaaaaaaaaaaa", "GET /", SpanKind.SERVER, "app1")
        recordSpan("00000000000000000000000000000002", "bbbbbbbbbbbbbbbb", "GET /other", SpanKind.SERVER, "app1")

        approver.assertApproved(renderer("TestClass.testMethod"))
    }

    private fun recordSpan(traceId: String, spanId: String, name: String, kind: SpanKind, serviceName: String) {
        traceStore.record(testSpanData(traceId, spanId, name, kind = kind, serviceName = serviceName))
    }
}
