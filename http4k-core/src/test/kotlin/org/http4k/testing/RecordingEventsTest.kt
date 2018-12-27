package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Event
import org.http4k.core.Event.Companion.Error
import org.http4k.core.EventCategory
import org.junit.jupiter.api.Test

class RecordingEventsTest {
    private data class MyEvent(val message: String) : Event {
        override val category = EventCategory("boo")
    }

    @Test
    fun `can get list of events back from recording events`() {
        val recordingEvents = RecordingEvents()
        val first = MyEvent("foo")
        val second = MyEvent("bar")
        val third = Error("ohno")

        recordingEvents(first)
        recordingEvents(second)
        recordingEvents(third)

        assertThat(recordingEvents.toList(), equalTo(listOf(first, second, third)))
    }
}