/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.junit.jupiter.api.Test

class StoryboardOpenTelemetryTest {

    private val app: HttpHandler = { Response(OK).body("hi") }

    private fun tracedClient(downstream: HttpHandler): Pair<HttpHandler, SpanSnapshotStore> {
        val store = SpanSnapshotStore()
        val otel = StoryboardOpenTelemetry(store)
        return ClientFilters.OpenTelemetryTracing(otel).then(downstream) to store
    }

    @Test
    fun `the OTel client filter captures a CLIENT span for every outbound request`() {
        val (client, store) = tracedClient(app)
        client(Request(GET, "http://localhost/one"))
        client(Request(GET, "http://localhost/two"))

        val clientSpans = store.snapshots.filter { it.kind == "CLIENT" }
        assertThat(clientSpans.size, greaterThanOrEqualTo(2))
    }

    @Test
    fun `outbound requests carry W3C traceparent so downstream services can join the trace`() {
        var seen: String? = null
        val (client, _) = tracedClient { req ->
            seen = req.header("traceparent")
            Response(OK)
        }

        client(Request(GET, "http://localhost/x"))

        assertThat(seen != null, equalTo(true))
        assertThat(seen!!.startsWith("00-"), equalTo(true))
    }

    @Test
    fun `spans drain ordered by start time`() {
        val (client, store) = tracedClient(app)
        client(Request(GET, "http://localhost/a"))
        client(Request(GET, "http://localhost/b"))
        client(Request(GET, "http://localhost/c"))

        val starts = store.snapshots.map { it.startEpochNanos }
        assertThat(starts, equalTo(starts.sorted()))
    }

    @Test
    fun `an idle store produces no spans`() {
        val store = SpanSnapshotStore()
        StoryboardOpenTelemetry(store)

        assertThat(store.snapshots, equalTo(emptyList()))
    }
}
