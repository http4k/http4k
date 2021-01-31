package org.http4k.events

import org.http4k.filter.ZipkinTraces
import java.time.Clock

/**
 * Useful EventFilters used in building event processing pipelines to add various types of metadata to the events
 */
object EventFilters {
    /**
     * Adds timestamp metadata to the event.
     */
    fun AddTimestamp(clock: Clock = Clock.systemUTC()) = EventFilter { next ->
        {
            next(it + ("timestamp" to clock.instant()))
        }
    }

    /**
     * Adds Zipkin traces metadata to the event.
     */
    fun AddZipkinTraces() = EventFilter { next ->
        {
            next(it + ("traces" to ZipkinTraces.THREAD_LOCAL.get()))
        }
    }
}
