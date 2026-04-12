/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.util.FixedClock
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.Ordering.Ascending
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.otel.WiretapOpenTelemetry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration

@ExtendWith(ApprovalTest::class)
class TestReportRendererTest {

    private val app = routes("/" bind Method.GET to { Response(OK).body("hello") })

    private fun renderer(
        traceStore: TraceStore = TraceStore.InMemory(),
        logStore: LogStore = LogStore.InMemory(),
        transactionStore: TransactionStore = TransactionStore.InMemory()
    ) = TestReportRenderer(traceStore, logStore, transactionStore, FixedClock)

    @Test
    fun `renders empty report`(approver: Approver) {
        approver.assertApproved(renderer()("Empty.test"))
    }

    @Test
    fun `renders stdout and stderr tabs`(approver: Approver) {
        approver.assertApproved(renderer()("Output.test", stdOut = "stdout line", stdErr = "stderr line"))
    }

    @Test
    fun `renders traffic tab with recorded transactions`(approver: Approver) {
        val transactionStore = TransactionStore.InMemory()
        transactionStore.record(
            HttpTransaction(
                request = Request(Method.GET, "/api/test"),
                response = Response(OK).body("test response"),
                start = FixedClock.instant(),
                duration = Duration.ofMillis(42)
            ),
            Direction.Inbound
        )

        approver.assertApproved(renderer(transactionStore = transactionStore)("Traffic.test"))
    }

    @Test
    fun `renders multiple traces into a single report`() {
        val traceStore = TraceStore.InMemory()
        val logStore = LogStore.InMemory()
        val handler = ServerFilters.OpenTelemetryTracing(WiretapOpenTelemetry(traceStore, logStore, FixedClock)).then(app)

        handler(Request(Method.GET, "/"))
        handler(Request(Method.GET, "/"))

        assertThat(traceStore.traces(Ascending).size, equalTo(2))

        val content = renderer(traceStore, logStore)("TestClass.testMethod")
        traceStore.traces(Ascending).keys.forEach { traceId ->
            assertThat("report contains trace $traceId", content.contains(traceId.value), equalTo(true))
        }
    }
}
