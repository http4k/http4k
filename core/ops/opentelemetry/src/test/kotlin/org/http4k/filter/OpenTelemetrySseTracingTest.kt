package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.TraceId
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.data.SpanData
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class OpenTelemetrySseTracingTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            GlobalOpenTelemetry.resetForTest()
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

        val app = ServerFilters.OpenTelemetrySseTracing(spanCompletionMutator = { span, _, _ ->
            createdContext = (span as ReadableSpan).toSpanData()
        })
            .then(sse("/foo/{id}" bind {
                SseResponse { it.close() }
            }))

        val response = app(
            Request(GET, "http://localhost:8080/foo/bar?a=b")
                .header("x-b3-traceid", sentTraceId)
                .header("x-b3-spanid", parentSpanId)
                .header("x-b3-sampled", "1")
        )

        response.consumer.invoke(testSse())

        with(createdContext!!) {
            assertThat(attributes.get(stringKey("http.method")), equalTo("GET"))
            assertThat(attributes.get(stringKey("http.url")), equalTo("http://localhost:8080/foo/bar?a=b"))
            assertThat(attributes.get(stringKey("http.route")), equalTo("foo/{id}"))
            assertThat(traceId, equalTo(sentTraceId))
            assertThat(spanId, !equalTo(parentSpanId))
            assertThat(this.parentSpanId, equalTo(parentSpanId))
        }
    }

    @Test
    fun `server creates new span when no parent`() {
        var createdContext: SpanData? = null

        val app = ServerFilters.OpenTelemetrySseTracing(spanCompletionMutator = { span, _, _ ->
            createdContext = (span as ReadableSpan).toSpanData()
        })
            .then(sse("/foo/{id}" bind {
                SseResponse { it.close() }
            }))

        val response = app(Request(GET, "http://localhost:8080/foo/bar?a=b"))

        response.consumer.invoke(testSse())

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
    fun `a server span can be mutated during creation`() {
        val sentTraceId = "11111111111111111111111111111111"
        val parentSpanId = "2222222222222222"
        val creationValue = stringKey("test-attribute")

        var createdContext: SpanData? = null

        val app = ServerFilters.OpenTelemetrySseTracing(
            spanCreationMutator = { spanBuilder, request ->
                spanBuilder.setAttribute(creationValue, request.header("x-its-a-me") ?: "no-its-a-not")
            },
            spanCompletionMutator = { span, _, _ ->
                createdContext = (span as ReadableSpan).toSpanData()
            }
        )
            .then(sse("/foo/{id}" bind {
                SseResponse { it.close() }
            }))

        val response = app(
            Request(GET, "http://localhost:8080/foo/bar?a=b")
                .header("x-b3-traceid", sentTraceId)
                .header("x-b3-spanid", parentSpanId)
                .header("x-b3-sampled", "1")
                .header("x-its-a-me", "mario")
        )

        response.consumer.invoke(testSse())

        with(createdContext!!) {
            assertThat(attributes.get(creationValue), equalTo("mario"))
        }
    }

    @Test
    fun `a server span can be mutated before completion`() {
        val sentTraceId = "11111111111111111111111111111111"
        val parentSpanId = "2222222222222222"
        val postStatus = longKey("post-status")
        val postUri = stringKey("post-uri")

        var currentSpan: SpanData? = null

        val app = ServerFilters.OpenTelemetrySseTracing(spanCompletionMutator = { span, request, response ->
            span.setAttribute(postStatus, response.status.code)
            span.setAttribute(postUri, request.uri.toString())

            currentSpan = (span as ReadableSpan).toSpanData()
        })
            .then(sse("/foo/{id}" bind {
                SseResponse { it.close() }
            }))

        val response = app(
            Request(GET, "http://localhost:8080/foo/bar?a=b")
                .header("x-b3-traceid", sentTraceId)
                .header("x-b3-spanid", parentSpanId)
                .header("x-b3-sampled", "1")
        )

        response.consumer.invoke(testSse())

        with(currentSpan!!) {
            assertThat(attributes.get(postStatus), equalTo(200L))
            assertThat(attributes.get(postUri), equalTo("http://localhost:8080/foo/bar?a=b"))
        }
    }

    @Test
    fun `span ends when SSE connection closes`() {
        var spanEndedBeforeClose = true
        var spanEndedAfterClose = false
        var capturedSpan: ReadableSpan? = null

        val app = ServerFilters.OpenTelemetrySseTracing(spanCompletionMutator = { span, _, _ ->
            capturedSpan = span as ReadableSpan
            spanEndedBeforeClose = span.hasEnded()
        })
            .then(sse("/foo" bind {
                SseResponse { it.close() }
            }))

        val response = app(Request(GET, "http://localhost:8080/foo"))

        response.consumer.invoke(testSse())

        spanEndedAfterClose = capturedSpan?.hasEnded() ?: false

        assertThat("span should not have ended before completion mutator", spanEndedBeforeClose, equalTo(false))
        assertThat("span should have ended after close", spanEndedAfterClose, equalTo(true))
    }

    private fun testSse() = object : Sse {
        override val connectRequest: Request = Request(GET, "/")
        override fun send(message: SseMessage) = this
        override fun close() {}
        override fun onClose(fn: () -> Unit) = this
    }
}
