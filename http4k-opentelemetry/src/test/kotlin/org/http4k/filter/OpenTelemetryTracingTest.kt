package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.endsWith
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.OpenTelemetry
import io.opentelemetry.context.propagation.DefaultContextPropagators.builder
import io.opentelemetry.extensions.trace.propagation.B3Propagator.getMultipleHeaderPropagator
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Test


class OpenTelemetryTracingTest {

    @Test
    fun `server and client context propogates when existing span`() {
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

        val traceId = "11111111111111111111111111111111"
        val spanId = "2222222222222222"

        val message = app(Request(GET, "http://localhost:8080/foo/bar?a=b")
            .header("x-b3-traceid", traceId)
            .header("x-b3-spanid", spanId)
            .header("x-b3-sampled", "1")
        )

        assertThat(message, hasHeader("x-b3-traceid", endsWith(traceId)))
        assertThat(message, hasHeader("x-b3-spanid", !equalTo(spanId)))
        assertThat(message, hasHeader("x-b3-sampled", equalTo("1")))

    }
}
