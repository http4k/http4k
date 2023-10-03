package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.testing.RecordingEvents
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test

class EventFiltersTests {

    private val recording = RecordingEvents()

    @Test
    fun `AddEventName captures name of event`() {
        val events = EventFilters.AddEventName().then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("name" to "MyEvent")))))
    }

    @Test
    fun `AddServiceName captures name of event`() {
        val events = EventFilters.AddServiceName("bob").then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("service" to "bob")))))
    }

    @Test
    fun `AddEventName captures name of metadata wrapped event`() {
        val events = EventFilters.AddEventName().then(recording)
        val event = MyEvent()

        events(MetadataEvent(event))

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("name" to "MyEvent")))))
    }

    @Test
    fun `AddTimestamp captures instant`() {
        val clock = FixedClock
        val events = EventFilters.AddTimestamp(clock).then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("timestamp" to clock.instant())))))
    }

    @Test
    fun `AddZipkinTraces captures traces`() {
        val expected = ZipkinTracesStorage.THREAD_LOCAL.forCurrentThread()
        val events = EventFilters.AddZipkinTraces().then(recording)
        val event = MyEvent()

        events(event)

        assertThat(recording.toList(), equalTo(listOf<Event>(MetadataEvent(event, mapOf("traces" to expected)))))
    }
}
