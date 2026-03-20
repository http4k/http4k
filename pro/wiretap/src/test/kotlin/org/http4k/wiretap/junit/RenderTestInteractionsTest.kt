package org.http4k.wiretap.junit

import App
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.trace.SpanKind
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import testRequest

class RenderTestInteractionsTest {

    private val downstream: HttpHandler = { Response(OK).body("downstream") }

    @RegisterExtension
    @JvmField
    val wiretap = RenderTestInteractions({ App(http(), otel("test app 1"), "test app 1") }, downstream, Always)

    @Test
    fun `requests through factory-built app reach the app`(http: HttpHandler) {
        val response = http(testRequest())
        assertThat(response.bodyString(), equalTo("downstream"))
    }

    @Test
    fun `captures both server and client spans`(http: HttpHandler) {
        http(testRequest())

        val spans = wiretap.traceStore.traces().values.first()
        assertThat(spans.any { it.kind == SpanKind.SERVER }, equalTo(true))
        assertThat(spans.any { it.kind == SpanKind.CLIENT }, equalTo(true))
    }

    @Test
    fun `captures traffic for each request`(http: HttpHandler) {
        http(testRequest())

        val traffic = wiretap.transactionStore.list()
        assertThat(traffic.size, equalTo(1))
        assertThat(traffic.first().transaction.request.method, equalTo(GET))
    }

    @Test
    fun `captures stdout and stderr during test execution`(http: HttpHandler) {
        http(testRequest())

        assertThat("stdout has event JSON", wiretap.capturedStdOut.contains("user-42"), equalTo(true))
        assertThat("stderr has app warning", wiretap.capturedStdErr.contains("downstream warning"), equalTo(true))
    }

    @Test
    fun `renders multiple traces into a single report`(http: HttpHandler) {
        http(testRequest())
        http(testRequest())

        assertThat(wiretap.traceStore.traces().size, equalTo(2))

        val file = renderTestReport("TestClass.testMethod", "org/http4k/wiretap/junit", wiretap.traceStore, wiretap.logStore, wiretap.transactionStore)
        assertThat(file.name, equalTo("TestClass.testMethod.html"))

        val content = file.readText()
        wiretap.traceStore.traces().keys.forEach { traceId ->
            assertThat("report contains trace $traceId", content.contains(traceId.value), equalTo(true))
        }
    }
}
