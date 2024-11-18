package org.http4k.tracing

import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.tracing.junit.RecordingMode

interface VcrEvents : Events, Iterable<Event> {

    /**
     * Enable trace recording for just this block.
     */
    fun <T> record(block: () -> T): T

    /**
     * Disable trace recording for just this block.
     */
    fun <T> pause(block: () -> T): T

    /**
     * Enable trace recording
     */
    fun resume()

    /**
     * Disable trace recording
     */
    fun pause()
}
