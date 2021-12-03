package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.events.Event.Companion.Error
import org.http4k.testing.RecordingEvents
import org.junit.jupiter.api.Test

class EventsTest {
    @Test
    fun `error has correct category`() {
        assertThat(Error("oh no", RuntimeException("foo")).category, equalTo(EventCategory("error")))
    }

    @Test
    fun `can combine events implementations with and()`() {
        val first = RecordingEvents()
        val second = RecordingEvents()
        val error = Error("foo")

        first.then(second)(error)

        assertThat(first.toList(), equalTo(listOf<Event>(error)))
        assertThat(second.toList(), equalTo(listOf<Event>(error)))
    }
}
