package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.AutoOpenTelemetryEvents
import org.http4k.events.Event
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryTracing
import org.http4k.filter.ServerFilters
import org.http4k.format.Moshi
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

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

fun testRequest() = Request(GET, "/")
    .header("Accept", "application/json")
    .header("Authorization", "Bearer test-token")
    .header("X-Request-Id", "req-abc-123")
    .header("User-Agent", "http4k-test/1.0")
    .query("page", "1")
    .query("limit", "25")
    .query("sort", "created_at")

data class RequestProcessed(val path: String, val userId: String) : Event

fun App(httpClient: HttpHandler, oTel: OpenTelemetry, name: String): RoutingHttpHandler {
    val tracer = oTel.tracerProvider.get(name)
    val tracedClient = ClientFilters.OpenTelemetryTracing(oTel).then(httpClient)
    val logger = oTel.logsBridge.get(name)
    val events: (Event) -> Unit = { event ->
        AutoOpenTelemetryEvents(Moshi, oTel)(event)
        AutoMarshallingEvents(Moshi)(event)
    }
    return ServerFilters.OpenTelemetryTracing(oTel)
        .then(routes("/{path:.*}" bind GET to { req ->
            Baggage.current().toBuilder()
                .put("user.id", "user-42")
                .put("session.id", "sess-abc123")
                .build()
                .makeCurrent().use {
                    Span.current().setAttribute("app.request.id", "test-123")

                    val validateSpan = tracer.spanBuilder("validate-request").startSpan()
                    validateSpan.makeCurrent().use {
                        validateSpan.setAttribute("validation.result", "passed")
                        validateSpan.addEvent("request-validated")
                    }
                    validateSpan.end()

                    logger.logRecordBuilder()
                        .setBody("handling request")
                        .setAttribute(io.opentelemetry.api.common.AttributeKey.stringKey("log.level"), "INFO")
                        .emit()
                    events(RequestProcessed(req.uri.path, "user-42"))
                    System.err.println("downstream warning: slow response detected")

                    val fetchSpan = tracer.spanBuilder("fetch-downstream").startSpan()
                    val result = fetchSpan.makeCurrent().use {
                        tracedClient(
                            Request(GET, "/downstream")
                                .header("Accept", "application/json")
                                .header("X-Correlation-Id", "corr-001")
                        )
                    }
                    fetchSpan.end()
                    result
                }
        }))
}
