package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter.create
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.then
import org.http4k.core.Uri
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.junit.jupiter.api.Test

class ResourceOpenTelemetryTracingTest {

    private val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(create()))
                .build()
        )
        .setPropagators(ContextPropagators.create(B3Propagator.injectingMultiHeaders()))
        .build()


    @Test
    fun `sets attributes on current span`() {
        val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)
        val parentSpan = tracer.spanBuilder("resources/read").startSpan()

        var capturedResourceName: String? = null
        var capturedOperationName: String? = null

        val handler = ResourceFilters.OpenTelemetryTracing(ResourceName.of("my-resource")).then {
            val attrs = (Span.current() as ReadableSpan).toSpanData().attributes
            capturedResourceName = attrs.get(stringKey("gen_ai.resource.name"))
            capturedOperationName = attrs.get(stringKey("gen_ai.operation.name"))
            ResourceResponse()
        }

        parentSpan.makeCurrent().use {
            handler(ResourceRequest(Uri.of("file:///test")))
        }

        parentSpan.end()

        assertThat(capturedResourceName, equalTo("my-resource"))
        assertThat(capturedOperationName, equalTo("read_resource"))
    }
}
