/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.B3Propagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.McpRequest
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.then
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.MoshiObject
import org.http4k.format.renderError
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
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
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry = openTelemetry)

        var capturedSpan: SpanData? = null

        val handler = filter.then {
            capturedSpan = (Span.current() as ReadableSpan).toSpanData()
            McpResponse.Ok(McpJson.nullNode())
        }

        val session = Session(SessionId.of("test-session-123"))

        handler(mcpRequest(session))

        with(capturedSpan!!) {
            assertThat(name, equalTo("tools/call test"))
            assertThat(kind, equalTo(SpanKind.SERVER))
            assertThat(attributes.get(AttributeKey.stringKey("mcp.method.name")), equalTo("tools/call"))
            assertThat(attributes.get(AttributeKey.stringKey("mcp.session.id")), equalTo("test-session-123"))
            assertThat(attributes.get(AttributeKey.stringKey("jsonrpc.request.id")), equalTo("1"))
            assertThat(attributes.get(AttributeKey.stringKey("mcp.protocol.version")), equalTo("2025-11-25"))
        }
    }

    @Test
    fun `sets error status when handler throws`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry = openTelemetry)

        val handler = filter.then { throw IllegalStateException("boom") }

        runCatching { handler(mcpRequest()) }

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.status.statusCode, equalTo(StatusCode.ERROR))
        assertThat(
            span.attributes.get(AttributeKey.stringKey("error.type")),
            equalTo("java.lang.IllegalStateException")
        )
    }

    @Test
    fun `sets error status when response is JSON-RPC error`() {
        val filter = McpFilters.OpenTelemetryTracing(openTelemetry = openTelemetry)

        val handler = filter.then {
            McpResponse.Ok(McpJson.renderError(ErrorMessage.InternalError, it.message.id))
        }

        handler(mcpRequest())

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.status.statusCode, equalTo(StatusCode.ERROR))
        assertThat(span.attributes.get(AttributeKey.stringKey("error.type")), equalTo("-32603"))
    }

    @Test
    fun `links to transport span when present`() {
        val mcpHandler =
            McpFilters.OpenTelemetryTracing(openTelemetry = openTelemetry).then { McpResponse.Ok(McpJson.nullNode()) }

        val poly = PolyFilters.OpenTelemetryTracing(openTelemetry).then(
            PolyHandler(http = { req ->
                mcpHandler(mcpRequest(http = req))
                Response(OK)
            })
        )

        poly.http!!(Request(POST, "/mcp"))

        val spans = spanExporter.finishedSpanItems
        assertThat(spans.size, equalTo(2))

        val transportSpan = spans.first { it.kind == SpanKind.SERVER && it.name != "tools/call test" }
        val mcpSpan = spans.first { it.name == "tools/call test" }

        assertThat(mcpSpan.links.size, equalTo(1))
        assertThat(mcpSpan.links.first().spanContext.traceId, equalTo(transportSpan.spanContext.traceId))
        assertThat(mcpSpan.links.first().spanContext.spanId, equalTo(transportSpan.spanContext.spanId))
        assertThat(mcpSpan.parentSpanId, equalTo(SpanId.getInvalid()))
    }

    @Test
    fun `extracts trace context from _meta`() {
        val w3cOpenTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .build()
            )
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()

        val filter = McpFilters.OpenTelemetryTracing(openTelemetry = w3cOpenTelemetry)

        val handler = filter.then { McpResponse.Ok(McpJson.nullNode()) }

        val parentTraceId = "0af7651916cd43dd8448eb211c80319c"
        val parentSpanId = "b7ad6b7169203331"

        val meta = Meta(MoshiObject(
            "traceparent" to asJsonObject("00-$parentTraceId-$parentSpanId-01"),
            "tracestate" to asJsonObject("congo=t61rcWkgMzE")
        ))
        val message = McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("test"), _meta = meta), asJsonObject(1))

        handler(mcpRequest(message = message))

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.spanContext.traceId, equalTo(parentTraceId))
        assertThat(span.parentSpanId, equalTo(parentSpanId))
    }

    @Test
    fun `has no parent when _meta has no trace context`() {
        val w3cOpenTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                    .build()
            )
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()

        val filter = McpFilters.OpenTelemetryTracing(openTelemetry = w3cOpenTelemetry)

        val handler = filter.then { McpResponse.Ok(McpJson.nullNode()) }

        handler(mcpRequest())

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.parentSpanId, equalTo(SpanId.getInvalid()))
    }

    @Test
    fun `supports custom span modifiers for the same method`() {
        val filter = McpFilters.OpenTelemetryTracing(
            openTelemetry = openTelemetry,
            spanModifiers = listOf(
                spanModifier("request-attr-key-1" to "request-attr-value-1", "response-attr-key-1" to "response-attr-value-1", McpTool.Call.Method),
                spanModifier("request-attr-key-2" to "request-attr-value-2", "response-attr-key-2" to "response-attr-value-2", McpTool.Call.Method),
            )
        )
        val handler = filter.then {
            McpResponse.Ok(McpJson.nullNode())
        }

        handler(mcpRequest())

        val capturedSpan = spanExporter.finishedSpanItems.single()
        with(capturedSpan!!) {
            assertThat(attributes.get(AttributeKey.stringKey("request-attr-key-1")), equalTo("request-attr-value-1"))
            assertThat(attributes.get(AttributeKey.stringKey("response-attr-key-1")), equalTo("response-attr-value-1"))

            assertThat(attributes.get(AttributeKey.stringKey("request-attr-key-2")), equalTo("request-attr-value-2"))
            assertThat(attributes.get(AttributeKey.stringKey("response-attr-key-2")), equalTo("response-attr-value-2"))
        }
    }

    private fun mcpRequest(
        session: Session = Session(SessionId.of("test-session")),
        message: McpTool.Call.Request = McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of("test")), asJsonObject(1)),
        http: Request = Request(POST, "/mcp")
    ) = McpRequest(session, message, with(McpJson) { http.json(message) }.with(Header.MCP_PROTOCOL_VERSION of ProtocolVersion.LATEST_VERSION))

    private fun spanModifier(
        requestAttribute: Pair<String, String>,
        responseAttribute: Pair<String, String>,
        method: McpRpcMethod = McpTool.Call.Method
    ) = object : McpOpenTelemetrySpanModifiers {
        override val method: McpRpcMethod = method

        override fun request(sb: Span, request: McpNodeType) {
            sb.setAttribute(requestAttribute.first, requestAttribute.second)
        }

        override fun response(sb: Span, response: McpNodeType) {
            sb.setAttribute(responseAttribute.first, responseAttribute.second)
        }
    }
}
