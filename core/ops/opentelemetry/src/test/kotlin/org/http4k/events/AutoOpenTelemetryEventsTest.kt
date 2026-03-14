package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.http4k.format.Jackson
import org.junit.jupiter.api.Test

data class TestEvent(val name: String, val count: Int, val active: Boolean, val rate: Double) : Event

data class Nested(val inner: String)
data class ComplexEvent(val nested: Nested, val items: List<String>) : Event

data class Deep(val value: String)
data class Mid(val deep: Deep)
data class DeeplyNestedEvent(val mid: Mid) : Event

class AutoOpenTelemetryEventsTest {

    private val logExporter = InMemoryLogRecordExporter.create()
    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
        .build()
    private val otel = OpenTelemetrySdk.builder()
        .setLoggerProvider(
            SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
                .build()
        )
        .setTracerProvider(tracerProvider)
        .build()

    private val events = AutoOpenTelemetryEvents(Jackson, otel)

    @Test
    fun `event fields appear as typed LogRecord attributes when no active span`() {
        events(TestEvent("hello", 42, true, 3.14))

        val records = logExporter.finishedLogRecordItems
        assertThat(records.size, equalTo(1))

        val record = records.first()
        assertThat(record.attributes.get(AttributeKey.stringKey("name")), equalTo("hello"))
        assertThat(record.attributes.get(AttributeKey.longKey("count")), equalTo(42L))
        assertThat(record.attributes.get(AttributeKey.booleanKey("active")), equalTo(true))
        assertThat(record.attributes.get(AttributeKey.doubleKey("rate")), equalTo(3.14))
    }

    @Test
    fun `metadata event metadata appears as log attributes when no active span`() {
        val event = TestEvent("test", 1, false, 0.0) + ("service" to "my-service") + ("region" to "eu-west")

        events(event)

        val record = logExporter.finishedLogRecordItems.first()
        assertThat(record.attributes.get(AttributeKey.stringKey("service")), equalTo("my-service"))
        assertThat(record.attributes.get(AttributeKey.stringKey("region")), equalTo("eu-west"))
    }

    @Test
    fun `error events have ERROR severity in log records`() {
        events(Event.Companion.Error("something broke"))

        val record = logExporter.finishedLogRecordItems.first()
        assertThat(record.severity, equalTo(Severity.ERROR))
    }

    @Test
    fun `non-error events have INFO severity in log records`() {
        events(TestEvent("hello", 1, true, 0.0))

        val record = logExporter.finishedLogRecordItems.first()
        assertThat(record.severity, equalTo(Severity.INFO))
    }

    @Test
    fun `nested objects use dot-separated keys for attributes`() {
        events(ComplexEvent(Nested("value"), listOf("a", "b")))

        val record = logExporter.finishedLogRecordItems.first()
        assertThat(record.attributes.get(AttributeKey.stringKey("nested.inner")), equalTo("value"))
        assertThat(record.attributes.get(AttributeKey.stringKey("items")), equalTo("""["a","b"]"""))
    }

    @Test
    fun `deeply nested objects use dot-separated keys`() {
        events(DeeplyNestedEvent(Mid(Deep("hello"))))

        val record = logExporter.finishedLogRecordItems.first()
        assertThat(record.attributes.get(AttributeKey.stringKey("mid.deep.value")), equalTo("hello"))
    }

    @Test
    fun `event emitted with active span becomes a span event`() {
        val tracer = otel.getTracer("test")
        val span = tracer.spanBuilder("test-span").startSpan()
        val scope = span.makeCurrent()

        try {
            events(TestEvent("traced", 42, true, 3.14))
        } finally {
            scope.close()
            span.end()
        }

        assertThat(logExporter.finishedLogRecordItems.size, equalTo(0))

        val spans = spanExporter.finishedSpanItems
        assertThat(spans.size, equalTo(1))

        val spanEvents = spans.first().events
        assertThat(spanEvents.size, equalTo(1))

        val spanEvent = spanEvents.first()
        assertThat(spanEvent.name, equalTo("TestEvent"))
        assertThat(spanEvent.attributes.get(AttributeKey.stringKey("name")), equalTo("traced"))
        assertThat(spanEvent.attributes.get(AttributeKey.longKey("count")), equalTo(42L))
        assertThat(spanEvent.attributes.get(AttributeKey.booleanKey("active")), equalTo(true))
        assertThat(spanEvent.attributes.get(AttributeKey.doubleKey("rate")), equalTo(3.14))
    }

    @Test
    fun `metadata appears as span event attributes with active span`() {
        val tracer = otel.getTracer("test")
        val span = tracer.spanBuilder("test-span").startSpan()
        val scope = span.makeCurrent()

        try {
            events(TestEvent("test", 1, false, 0.0) + ("service" to "my-service"))
        } finally {
            scope.close()
            span.end()
        }

        val spanEvent = spanExporter.finishedSpanItems.first().events.first()
        assertThat(spanEvent.attributes.get(AttributeKey.stringKey("service")), equalTo("my-service"))
        assertThat(spanEvent.attributes.get(AttributeKey.stringKey("name")), equalTo("test"))
    }

}
