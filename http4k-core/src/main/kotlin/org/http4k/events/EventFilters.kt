package org.http4k.events

import java.time.Clock

/**
 * Useful EventFilters used in building event processing pipelines to add various types of metadata to the events
 */
object EventFilters {
    /**
     * Adds timestamp metadata to the event.
     */
    fun AddTimestamp(clock: Clock) = EventsFilter { next ->
        {
            next(MetadataEvent(it) + ("timestamp" to clock.instant()))
        }
    }
}