package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.RecordingEvents
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class EventsFiltersTests {

    private val recording = RecordingEvents()

    @Test
    fun `timed event captures instant`() {
        val clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault())
        val events = EventFilters.Timed(clock).then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("timestamp" to clock.instant())))))
    }
}