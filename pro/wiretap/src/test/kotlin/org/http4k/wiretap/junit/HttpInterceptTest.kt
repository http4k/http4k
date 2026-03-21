/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import App
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.trace.SpanKind
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Ordering.Ascending
import org.http4k.wiretap.domain.Ordering.Descending
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import testRequest

class HttpInterceptTest {

    private val downstream: HttpHandler = { Response(OK).body("downstream") }

    @RegisterExtension
    @JvmField
    val intercept = Intercept(downstream, Always) {
        App(http(), "test app 1", otel("test app 1"))
    }

    @Test
    fun `requests through factory-built app reach the app`(http: HttpHandler) {
        val response = http(testRequest())
        assertThat(response.bodyString(), equalTo("downstream"))
    }

    @Test
    fun `captures both server and client spans`(http: HttpHandler) {
        http(testRequest())

        val spans = intercept.traceStore.traces(Descending).values.first()
        assertThat(spans.any { it.kind == SpanKind.SERVER }, equalTo(true))
        assertThat(spans.any { it.kind == SpanKind.CLIENT }, equalTo(true))
    }

    @Test
    fun `captures traffic for each request`(http: HttpHandler) {
        http(testRequest())

        val traffic = intercept.transactionStore.list(Descending)
        assertThat(traffic.size, equalTo(3))

        val inbound = traffic.filter { it.direction == Direction.Inbound }
        assertThat(inbound.size, equalTo(1))
        assertThat(inbound.first().transaction.request.method, equalTo(GET))

        val outbound = traffic.filter { it.direction == Direction.Outbound }
        assertThat(outbound.size, equalTo(2))
    }

    @Test
    fun `captures stdout and stderr during test execution`(http: HttpHandler) {
        http(testRequest())

        assertThat("stdout has event JSON", intercept.capturedStdOut.contains("user-42"), equalTo(true))
        assertThat("stderr has app warning", intercept.capturedStdErr.contains("downstream warning"), equalTo(true))
    }

    @Test
    fun `renders multiple traces into a single report`(http: HttpHandler) {
        http(testRequest())
        http(testRequest())

        assertThat(intercept.traceStore.traces(Ascending).size, equalTo(2))

        val file = renderTestReport("TestClass.testMethod", "org/http4k/wiretap/junit", intercept.traceStore, intercept.logStore, intercept.transactionStore)
        assertThat(file.name, equalTo("TestClass.testMethod.html"))

        val content = file.readText()
        intercept.traceStore.traces(Ascending).keys.forEach { traceId ->
            assertThat("report contains trace $traceId", content.contains(traceId.value), equalTo(true))
        }
    }

}
