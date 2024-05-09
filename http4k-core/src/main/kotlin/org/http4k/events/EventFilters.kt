package org.http4k.events

import org.http4k.filter.ZipkinTracesStorage
import java.time.Clock
import java.time.Instant

/**
 * Useful EventFilters used in building event processing pipelines to add various types of metadata to the events
 */
object EventFilters {
    /**
     * Adds event name to the event.
     */
    fun AddEventName() = EventFilter { next ->
        {
            next(it + ("name" to when (it) {
                is MetadataEvent -> it.event.javaClass.simpleName
                else -> it.javaClass.simpleName
            }))
        }
    }

    /**
     * Adds a service name to the event.
     */
    fun AddServiceName(name: String) = EventFilter { next ->
        {
            next(it + ("service" to name))
        }
    }

    /**
     * Adds timestamp metadata to the event.
     */
    fun AddTimestamp(clock: Clock = Clock.systemUTC()) = AddTimestamp(clock::instant)

    /**
     * Adds timestamp metadata to the event.
     */
    fun AddTimestamp(timeSource: () -> Instant) = EventFilter { next ->
        {
            next(it + ("timestamp" to timeSource()))
        }
    }

    /**
     * Adds Zipkin traces metadata to the event.
     */
    fun AddZipkinTraces(storage: ZipkinTracesStorage = ZipkinTracesStorage.THREAD_LOCAL) = EventFilter { next ->
        {
            next(it + ("traces" to storage.forCurrentThread()))
        }
    }
}
