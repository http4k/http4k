package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.OpenTelemetry
import io.opentelemetry.common.AttributeKey.stringKey
import io.opentelemetry.context.propagation.DefaultContextPropagators.builder
import io.opentelemetry.extensions.trace.propagation.B3Propagator.getMultipleHeaderPropagator
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.trace.SpanId
import io.opentelemetry.trace.TraceId
import io.opentelemetry.trace.TracingContextUtils
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class OpenTelemetryTracingTest {

    private val tracer = OpenTelemetry.getTracer("http4k", "semver:0.0.0")

    init {
        OpenTelemetry.setPropagators(builder().addTextMapPropagator(getMultipleHeaderPropagator()).build())
    }

    @Test
    fun `server creates new span when existing trace`() {
        val sentTraceId = "11111111111111111111111111111111"
        val parentSpanId = "2222222222222222"

        var createdContext: SpanData? = null

        val app = ServerFilters.OpenTelemetryTracing(tracer)
            .then(routes("/foo/{:id}" bind GET to {
                createdContext = (TracingContextUtils.getCurrentSpan() as ReadableSpan).toSpanData()
                Response(OK)
            }))

        app(Request(GET, "http://localhost:8080/foo/bar?a=b")
            .header("x-b3-traceid", sentTraceId)
            .header("x-b3-spanid", parentSpanId)
            .header("x-b3-sampled", "1")
        )

        with(createdContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/foo/bar?a=b"))
            assertThat(attributes.get(stringKey("http.route")), equalTo("foo/{:id}"))
            assertThat(traceId, equalTo(sentTraceId))
            assertThat(spanId, !equalTo(parentSpanId))
            assertThat(parentSpanId, equalTo(parentSpanId))
            assertThat(isSampled, equalTo(true))
        }
    }

    @Test
    fun `server creates new span when no parent`() {
        var createdContext: SpanData? = null

        val app = ServerFilters.OpenTelemetryTracing(tracer)
            .then(routes("/foo/{:id}" bind GET to {
                createdContext = (TracingContextUtils.getCurrentSpan() as ReadableSpan).toSpanData()
                Response(OK)
            }))

        app(Request(GET, "http://localhost:8080/foo/bar?a=b"))

        with(createdContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/foo/bar?a=b"))
            assertThat(attributes.get(stringKey("http.route")), equalTo("foo/{:id}"))
            assertThat(traceId, !equalTo(TraceId.getInvalid()))
            assertThat(spanId, !equalTo(SpanId.getInvalid()))
            assertThat(parentSpanId, equalTo(SpanId.getInvalid()))
        }
    }

    @Test
    fun `server and client context propogates when existing span`() {
        val originalTraceId = "11111111111111111111111111111111"
        val spanId = "2222222222222222"

        val tracer = OpenTelemetry.getTracer("http4k", "semver:0.0.0")
        OpenTelemetry.setPropagators(builder().addTextMapPropagator(getMultipleHeaderPropagator()).build())

        val app = ServerFilters.OpenTelemetryTracing(tracer)
            .then(Filter { next ->
                {
                    // clean the request
                    next(Request(GET, ""))
                }
            })
            .then(ClientFilters.OpenTelemetryTracing(tracer))
            .then {
                Response(OK).headers(it.headers)
            }

        val message = app(Request(GET, "http://localhost:8080/foo/bar?a=b")
            .header("x-b3-traceid", originalTraceId)
            .header("x-b3-spanid", spanId)
            .header("x-b3-sampled", "1")
        )

        assertThat(message, hasHeader("x-b3-traceid", endsWith(originalTraceId)))
        assertThat(message, hasHeader("x-b3-spanid", !equalTo(spanId)))
        assertThat(message, hasHeader("x-b3-sampled", equalTo("1")))
    }
}
