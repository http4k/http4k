package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.ZipkinTraces
import org.http4k.testing.RecordingEvents
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test

class EventFiltersTests {

    private val recording = RecordingEvents()

    @Test
    fun `AddTimestamp captures instant`() {
        val clock = FixedClock
        val events = EventFilters.AddTimestamp(clock).then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("timestamp" to clock.instant())))))
    }

    @Test
    fun `AddZipkinTraces captures instant`() {
        val expected = ZipkinTraces.THREAD_LOCAL.get()
        val events = EventFilters.AddZipkinTraces().then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("traces" to expected)))))
    }
}
