/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.core.HttpTransaction
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.time.Instant

@ExtendWith(ApprovalTest::class)
class LivingDocRendererTest {

    val txStore = TransactionStore.InMemory()
    val traceStore = TraceStore.InMemory()
    val renderer = LivingDocRenderer(traceStore, txStore)

    @Test
    fun `empty stores produce just the heading`(approver: Approver) {
        approver.assertApproved(renderer("MyTest.does something"))
    }

    @Test
    fun `inbound and outbound transactions are grouped and formatted`(approver: Approver) {
        txStore.record(
            HttpTransaction(
                Request(POST, "/api/data")
                    .header("Content-Type", "application/json")
                    .header("Host", "myapp.com")
                    .body("""{"name":"test","value":42}"""),
                Response(OK)
                    .header("Content-Type", "application/json")
                    .body("""{"id":1,"name":"test","value":42}"""),
                Duration.ofMillis(10),
                start = Instant.EPOCH
            ),
            Inbound
        )
        txStore.record(
           HttpTransaction(
                Request(DELETE, "https://downstream.example.com/foobar")
                    .header("Host", "downstream.example.com"),
                Response(INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "text/plain")
                    .body("something went wrong"),
                Duration.ofMillis(5),
                start = Instant.EPOCH
            ),
            Outbound
        )

        approver.assertApproved(renderer("MyTest.calls downstream"))
    }
}
