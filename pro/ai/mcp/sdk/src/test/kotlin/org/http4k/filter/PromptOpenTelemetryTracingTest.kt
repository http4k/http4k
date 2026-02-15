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
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.then
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.junit.jupiter.api.Test

class PromptOpenTelemetryTracingTest {

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
        val parentSpan = tracer.spanBuilder("prompts/get").startSpan()

        var capturedPromptName: String? = null
        var capturedOperationName: String? = null

        val handler = PromptFilters.OpenTelemetryTracing(PromptName.of("my-prompt")).then {
            val attrs = (Span.current() as ReadableSpan).toSpanData().attributes
            capturedPromptName = attrs.get(stringKey("gen_ai.prompt.name"))
            capturedOperationName = attrs.get(stringKey("gen_ai.operation.name"))
            PromptResponse(emptyList())
        }

        parentSpan.makeCurrent().use {
            handler(PromptRequest())
        }

        parentSpan.end()

        assertThat(capturedPromptName, equalTo("my-prompt"))
        assertThat(capturedOperationName, equalTo("get_prompt"))
    }
}
