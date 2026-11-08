package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.testing.RecordingEvents
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test

class EventFiltersTest {

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

    @Test
    fun `Accept only passes events matching predicate`() {
        val events = EventFilters.Accept { it == MyEvent("keep") }.then(recording)

        events(MyEvent("keep"))
        events(MyEvent("drop"))

        assertThat(recording.toList(), equalTo(listOf<Event>(MyEvent("keep"))))
    }

    @Test
    fun `Reject drops events matching predicate`() {
        val events = EventFilters.Reject { it == MyEvent("drop") }.then(recording)

        events(MyEvent("keep"))
        events(MyEvent("drop"))

        assertThat(recording.toList(), equalTo(listOf<Event>(MyEvent("keep"))))
    }

    @Test
    fun `Accept by type passes matching events including metadata wrapped ones`() {
        val events = EventFilters.Accept<MyEvent>().then(recording)
        val wrapped = MetadataEvent(MyEvent(), mapOf("foo" to "bar"))

        events(MyEvent())
        events(wrapped)
        events(OtherEvent)

        assertThat(recording.toList(), equalTo(listOf(MyEvent(), wrapped)))
    }

    @Test
    fun `Reject by type drops matching events including metadata wrapped ones`() {
        val events = EventFilters.Reject<MyEvent>().then(recording)

        events(MyEvent())
        events(MetadataEvent(MyEvent()))
        events(OtherEvent)

        assertThat(recording.toList(), equalTo(listOf<Event>(OtherEvent)))
    }

    @Test
    fun `Accept by type applies predicate to unwrapped event`() {
        val events = EventFilters.Accept<MyEvent> { it.value == "keep" }.then(recording)
        val wrapped = MetadataEvent(MyEvent("keep"), mapOf("foo" to "bar"))

        events(wrapped)
        events(MetadataEvent(MyEvent("drop")))
        events(OtherEvent)

        assertThat(recording.toList(), equalTo(listOf<Event>(wrapped)))
    }

    @Test
    fun `Reject by type applies predicate to unwrapped event`() {
        val events = EventFilters.Reject<MyEvent> { it.value == "drop" }.then(recording)

        events(MetadataEvent(MyEvent("keep")))
        events(MetadataEvent(MyEvent("drop")))
        events(OtherEvent)

        assertThat(recording.toList(), equalTo(listOf(MetadataEvent(MyEvent("keep")), OtherEvent)))
    }

    @Test
    fun `Accept predicate sees metadata`() {
        val events = EventFilters.Accept { it is MetadataEvent && it.metadata["foo"] == "bar" }.then(recording)
        val wrapped = MetadataEvent(MyEvent(), mapOf("foo" to "bar"))

        events(wrapped)
        events(MyEvent())

        assertThat(recording.toList(), equalTo(listOf<Event>(wrapped)))
    }

}

private object OtherEvent : Event
