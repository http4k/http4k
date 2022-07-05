package org.http4k.testing

import org.http4k.events.Event
import org.http4k.events.Events
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Simple recording events that can be used in tests
 */
class RecordingEvents : Events, Iterable<Event> {
    private val received = CopyOnWriteArrayList<Event>()

    override fun iterator() = received.iterator()

    override fun invoke(p1: Event) {
        received += p1
    }

    override fun toString() = "Events:\n" + received.joinToString("\n")
}
