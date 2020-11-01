package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.SamplingDecision.Companion.DO_NOT_SAMPLE
import org.http4k.filter.TraceId
import org.http4k.filter.ZipkinTraces
import org.http4k.testing.RecordingEvents
import org.junit.jupiter.api.Test

class AddOpenTelemetryTracesTest {

    private val recording = RecordingEvents()

    @Test
    fun `AddOpenTelemetryTraces captures traces`() {
        val expected = ZipkinTraces(
            TraceId("00000000000000000000000000000000"),
            TraceId("0000000000000000"),
            null,
            DO_NOT_SAMPLE
        )

        val events = EventFilters.AddOpenTelemetryTraces().then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("traces" to expected)))))
    }
}
