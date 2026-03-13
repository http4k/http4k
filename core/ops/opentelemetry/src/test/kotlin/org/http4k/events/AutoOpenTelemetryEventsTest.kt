package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

data class TestEvent(val name: String, val count: Int, val active: Boolean, val rate: Double) : Event

data class Nested(val inner: String)
data class ComplexEvent(val nested: Nested, val items: List<String>) : Event

class AutoOpenTelemetryEventsTest {

    private val exporter = InMemoryLogRecordExporter.create()
    private val otel = OpenTelemetrySdk.builder()
        .setLoggerProvider(
            SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
                .build()
        )
        .build()

    private val events = AutoOpenTelemetryEvents(Jackson, otel)

    @Test
    fun `event fields appear as typed LogRecord attributes`() {
        events(TestEvent("hello", 42, true, 3.14))

        val records = exporter.finishedLogRecordItems
        assertThat(records.size, equalTo(1))

        val record = records.first()
        assertThat(record.bodyValue?.asString(), equalTo("""{"name":"hello","count":42,"active":true,"rate":3.14}"""))
        assertThat(record.attributes.get(AttributeKey.stringKey("name")), equalTo("hello"))
        assertThat(record.attributes.get(AttributeKey.longKey("count")), equalTo(42L))
        assertThat(record.attributes.get(AttributeKey.booleanKey("active")), equalTo(true))
        assertThat(record.attributes.get(AttributeKey.doubleKey("rate")), equalTo(3.14))
    }

    @Test
    fun `metadata event metadata appears as attributes`() {
        val event = TestEvent("test", 1, false, 0.0) + ("service" to "my-service") + ("region" to "eu-west")

        events(event)

        val record = exporter.finishedLogRecordItems.first()
        assertThat(record.bodyValue?.asString(), equalTo("""{"name":"test","count":1,"active":false,"rate":0}"""))
        assertThat(record.attributes.get(AttributeKey.stringKey("service")), equalTo("my-service"))
        assertThat(record.attributes.get(AttributeKey.stringKey("region")), equalTo("eu-west"))
    }

    @Test
    fun `error events have ERROR severity`() {
        events(Event.Companion.Error("something broke"))

        val record = exporter.finishedLogRecordItems.first()
        assertThat(record.severity, equalTo(Severity.ERROR))
    }

    @Test
    fun `non-error events have INFO severity`() {
        events(TestEvent("hello", 1, true, 0.0))

        val record = exporter.finishedLogRecordItems.first()
        assertThat(record.severity, equalTo(Severity.INFO))
    }

    @Test
    fun `trace context is captured from current OTel span`() {
        val spanContext = SpanContext.create(
            "0af7651916cd43dd8448eb211c80319c",
            "b7ad6b7169203331",
            TraceFlags.getSampled(),
            TraceState.getDefault()
        )
        val span = Span.wrap(spanContext)
        val scope = Context.current().with(span).makeCurrent()

        try {
            events(TestEvent("traced", 1, true, 0.0))
        } finally {
            scope.close()
        }

        val record = exporter.finishedLogRecordItems.first()
        assertThat(record.spanContext.traceId, equalTo("0af7651916cd43dd8448eb211c80319c"))
        assertThat(record.spanContext.spanId, equalTo("b7ad6b7169203331"))
    }

    @Test
    fun `nested objects become compact JSON string attributes`() {
        events(ComplexEvent(Nested("value"), listOf("a", "b")))

        val record = exporter.finishedLogRecordItems.first()
        assertThat(record.bodyValue?.asString(), equalTo("""{"nested":{"inner":"value"},"items":["a","b"]}"""))
        assertThat(record.attributes.get(AttributeKey.stringKey("nested")), equalTo("""{"inner":"value"}"""))
        assertThat(record.attributes.get(AttributeKey.stringKey("items")), equalTo("""["a","b"]"""))
    }
}
