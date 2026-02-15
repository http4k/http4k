package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey
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
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.then
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
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

        val handler = filter.then {
            capturedSpan = (Span.current() as ReadableSpan).toSpanData()
            McpResponse(McpJson.nullNode())
        }

        val session = Session(SessionId.of("test-session-123"))
        val jsonReq = jsonRpcRequest()

        handler(McpRequest(session, jsonReq, Request.Companion(POST, "/mcp")))

        with(capturedSpan!!) {
            assertThat(name, equalTo("tools/call"))
            assertThat(kind, equalTo(SpanKind.SERVER))
            assertThat(attributes.get(AttributeKey.stringKey("mcp.method.name")), equalTo("tools/call"))
            assertThat(attributes.get(AttributeKey.stringKey("mcp.session.id")), equalTo("test-session-123"))
            assertThat(attributes.get(AttributeKey.stringKey("jsonrpc.request.id")), equalTo("1"))
        }
    }

    @Test
    fun `sets error status when handler throws`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry)

        val handler = filter.then { throw IllegalStateException("boom") }

        val session = Session(SessionId.of("test-session"))
        val jsonReq = jsonRpcRequest()

        runCatching { handler(McpRequest(session, jsonReq, Request.Companion(POST, "/mcp"))) }

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.status.statusCode, equalTo(StatusCode.ERROR))
        assertThat(
            span.attributes.get(AttributeKey.stringKey("error.type")),
            equalTo("java.lang.IllegalStateException")
        )
    }

    @Test
    fun `sets error status when response is JSON-RPC error`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry)

        val handler = filter.then {
            McpResponse(McpJson.renderError(ErrorMessage.InternalError, it.json.id))
        }

        val session = Session(SessionId.of("test-session"))
        val jsonReq = jsonRpcRequest()

        handler(McpRequest(session, jsonReq, Request.Companion(POST, "/mcp")))

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.status.statusCode, equalTo(StatusCode.ERROR))
        assertThat(span.attributes.get(AttributeKey.stringKey("error.type")), equalTo("-32603"))
    }

    @Test
    fun `links to transport span when present`() {
        val mcpHandler = McpFilters.OpenTelemetryTracing(openTelemetry).then { McpResponse(McpJson.nullNode()) }

        val poly = PolyFilters.OpenTelemetryTracing(openTelemetry).then(
            PolyHandler(http = { req ->
                mcpHandler(McpRequest(Session(SessionId.of("test-session")), jsonRpcRequest(), req))
                Response(OK)
            })
        )

        poly.http!!(Request(POST, "/mcp"))

        val spans = spanExporter.finishedSpanItems
        assertThat(spans.size, equalTo(2))

        val transportSpan = spans.first { it.kind == SpanKind.SERVER && it.name != "tools/call" }
        val mcpSpan = spans.first { it.name == "tools/call" }

        assertThat(mcpSpan.links.size, equalTo(1))
        assertThat(mcpSpan.links.first().spanContext.traceId, equalTo(transportSpan.spanContext.traceId))
        assertThat(mcpSpan.links.first().spanContext.spanId, equalTo(transportSpan.spanContext.spanId))
        assertThat(mcpSpan.parentSpanContext.isValid, equalTo(false))
    }

    private fun jsonRpcRequest() = JsonRpcRequest(
        McpJson, mapOf(
            "jsonrpc" to asJsonObject("2.0"),
            "method" to asJsonObject("tools/call"),
            "id" to asJsonObject(1),
            "params" to asJsonObject(emptyMap<String, Any>())
        )
    )
}
