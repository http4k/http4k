package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.trace.ReadableSpan
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.then
import org.http4k.ai.model.ToolName
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME
import org.junit.jupiter.api.Test

class ToolOpenTelemetryTracingTest {

    @Test
    fun `sets attributes on current span`() {
        val tracer = setupOtelSdk().tracerProvider.get(INSTRUMENTATION_NAME)
        val parentSpan = tracer.spanBuilder("tools/call").startSpan()

        var capturedToolName: String? = null
        var capturedOperationName: String? = null

        val handler = ToolFilters.OpenTelemetryTracing(ToolName.of("my-tool")).then {
            val attrs = (Span.current() as ReadableSpan).toSpanData().attributes
            capturedToolName = attrs.get(stringKey("gen_ai.tool.name"))
            capturedOperationName = attrs.get(stringKey("gen_ai.operation.name"))
            ToolResponse.Ok("result")
        }

        parentSpan.makeCurrent().use {
            handler(ToolRequest())
        }

        parentSpan.end()

        assertThat(capturedToolName, equalTo("my-tool"))
        assertThat(capturedOperationName, equalTo("execute_tool"))
    }

    @Test
    fun `sets error type and span status for tool error response`() {
        val tracer = setupOtelSdk().tracerProvider.get(INSTRUMENTATION_NAME)
        val parentSpan = tracer.spanBuilder("tools/call").startSpan()

        val handler = ToolFilters.OpenTelemetryTracing(ToolName.of("my-tool")).then {
            ToolResponse.Error("something went wrong")
        }

        parentSpan.makeCurrent().use {
            handler(ToolRequest())
        }

        parentSpan.end()

        val spanData = inMemorySpanExporter.finishedSpanItems.first()
        assertThat(spanData.attributes.get(stringKey("error.type")), equalTo("tool_error"))
        assertThat(spanData.status.statusCode, equalTo(StatusCode.ERROR))
    }
}
