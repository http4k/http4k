package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.renderError
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.junit.jupiter.api.Test

class McpOpenTelemetryTracingTest {

    private val spanExporter = InMemorySpanExporter.create()

    private val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(
            SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build()
        )
        .setPropagators(ContextPropagators.create(B3Propagator.injectingMultiHeaders()))
        .build()

    @Test
    fun `creates span with MCP attributes for successful request`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry)

        var capturedSpan: SpanData? = null

        val handler = filter.then { req ->
            capturedSpan = (Span.current() as ReadableSpan).toSpanData()
            McpResponse(McpJson.nullNode())
        }

        val session = Session(SessionId.of("test-session-123"))
        val jsonReq = jsonRpcRequest("tools/call", "42")

        handler(McpRequest(session, jsonReq, Request(POST, "/mcp")))

        with(capturedSpan!!) {
            assertThat(name, equalTo("tools/call"))
            assertThat(kind, equalTo(SpanKind.INTERNAL))
            assertThat(attributes.get(stringKey("mcp.method.name")), equalTo("tools/call"))
            assertThat(attributes.get(stringKey("mcp.session.id")), equalTo("test-session-123"))
            assertThat(attributes.get(stringKey("jsonrpc.request.id")), equalTo("\"42\""))
        }
    }

    @Test
    fun `sets error status when handler throws`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry)

        val handler = filter.then { throw IllegalStateException("boom") }

        val session = Session(SessionId.of("test-session"))
        val jsonReq = jsonRpcRequest("tools/call", "1")

        runCatching { handler(McpRequest(session, jsonReq, Request(POST, "/mcp"))) }

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.status.statusCode, equalTo(StatusCode.ERROR))
        assertThat(span.attributes.get(stringKey("error.type")), equalTo("java.lang.IllegalStateException"))
    }

    @Test
    fun `sets error status when response is JSON-RPC error`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry)

        val handler = filter.then {
            McpResponse(McpJson.renderError(ErrorMessage.InternalError, it.json.id))
        }

        val session = Session(SessionId.of("test-session"))
        val jsonReq = jsonRpcRequest("tools/call", "1")

        handler(McpRequest(session, jsonReq, Request(POST, "/mcp")))

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.status.statusCode, equalTo(StatusCode.ERROR))
        assertThat(span.attributes.get(stringKey("error.type")), equalTo("-32603"))
    }

    private fun jsonRpcRequest(method: String, id: String) = JsonRpcRequest(
        McpJson, mapOf(
            "jsonrpc" to McpJson.asJsonObject("2.0"),
            "method" to McpJson.asJsonObject(method),
            "id" to McpJson.asJsonObject(id),
            "params" to asJsonObject(emptyMap<String, Any>())
        )
    )
}
