package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.TraceId
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.data.SpanData
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OpenTelemetryTracingTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            OpenTelemetrySdk.builder()
                .setPropagators(ContextPropagators.create(B3Propagator.injectingMultiHeaders()))
                .buildAndRegisterGlobal()
        }
    }

    @Test
    fun `server creates new span when existing trace`() {
        val sentTraceId = "11111111111111111111111111111111"
        val parentSpanId = "2222222222222222"

        var createdContext: SpanData? = null

        val app = ServerFilters.OpenTelemetryTracing()
            .then(routes("/foo/{id}" bind GET to {
                createdContext = (Span.current() as ReadableSpan).toSpanData()
                Response(OK)
            }))

        val resp = app(
            Request(GET, "http://localhost:8080/foo/bar?a=b")
                .header("x-b3-traceid", sentTraceId)
                .header("x-b3-spanid", parentSpanId)
                .header("x-b3-sampled", "1")
        )

        assertThat(resp, hasHeader("x-b3-traceid", equalTo(sentTraceId)))
        assertThat(resp, hasHeader("x-b3-spanid", !equalTo(TraceId.getInvalid())))
        assertThat(resp, hasHeader("x-b3-sampled", equalTo("1")))

        with(createdContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/foo/bar?a=b"))
            assertThat(attributes.get(stringKey("http.route")), equalTo("foo/{id}"))
            assertThat(traceId, equalTo(sentTraceId))
            assertThat(spanId, !equalTo(parentSpanId))
            assertThat(this.parentSpanId, equalTo(parentSpanId))
//            assertThat(this.status, equalTo(true))
        }
    }

    @Test
    fun `server creates new span when no parent`() {
        var createdContext: SpanData? = null

        val app = ServerFilters.OpenTelemetryTracing()
            .then(routes("/foo/{id}" bind GET to {
                Span.current().spanContext
                createdContext = (Span.current() as ReadableSpan).toSpanData()
                Response(OK)
            }))

        val resp = app(Request(GET, "http://localhost:8080/foo/bar?a=b"))

        assertThat(resp, hasHeader("x-b3-traceid", !equalTo(TraceId.getInvalid())))
        assertThat(resp, hasHeader("x-b3-spanid", !equalTo(TraceId.getInvalid())))
        assertThat(resp, hasHeader("x-b3-sampled", equalTo("1")))

        with(createdContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/foo/bar?a=b"))
            assertThat(attributes.get(stringKey("http.route")), equalTo("foo/{id}"))
            assertThat(traceId, !equalTo(TraceId.getInvalid()))
            assertThat(spanId, !equalTo(SpanId.getInvalid()))
            assertThat(parentSpanId, equalTo(SpanId.getInvalid()))
        }
    }

    @Test
    fun `client creates new span when no parent`() {
        var createdContext: SpanData? = null

        val app = ClientFilters.OpenTelemetryTracing()
            .then {
                createdContext = (Span.current() as ReadableSpan).toSpanData()
                Response(I_M_A_TEAPOT)
            }

        app(Request(GET, "http://localhost:8080/foo/bar?a=b"))

        with(createdContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/foo/bar?a=b"))
            assertThat(traceId, !equalTo(TraceId.getInvalid()))
            assertThat(spanId, !equalTo(SpanId.getInvalid()))
            assertThat(parentSpanId, equalTo(SpanId.getInvalid()))
        }
    }

    @Test
    fun `server and client propagate correctly`() {
        val sentTraceId = "11111111111111111111111111111111"
        val originalSpanId = "2222222222222222"

        var serverContext: SpanData? = null
        var clientContext: SpanData? = null

        val app = ServerFilters.OpenTelemetryTracing()
            .then(Filter { next ->
                {
                    serverContext = (Span.current() as ReadableSpan).toSpanData()
                    next(Request(GET, "http://localhost:8080/client"))
                }
            })
            .then(ClientFilters.OpenTelemetryTracing())
            .then {
                clientContext = (Span.current() as ReadableSpan).toSpanData()
                Response(I_M_A_TEAPOT)
            }

        app(
            Request(GET, "http://localhost:8080/server")
                .header("x-b3-traceid", sentTraceId)
                .header("x-b3-spanid", originalSpanId)
                .header("x-b3-sampled", "1")
        )

        with(serverContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/server"))
            assertThat(traceId, equalTo(sentTraceId))
            assertThat(spanId, !equalTo(parentSpanId))
            assertThat(parentSpanId, equalTo(originalSpanId))
        }

        with(clientContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/client"))
            assertThat(traceId, equalTo(sentTraceId))
            assertThat(spanId, !equalTo(serverContext!!.spanId))
            assertThat(parentSpanId, equalTo(serverContext!!.spanId))
        }
    }
}
