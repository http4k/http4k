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
     * Only passes events which match the predicate.
     */
    fun Accept(predicate: (Event) -> Boolean) = EventFilter { next ->
        {
            if (predicate(it)) next(it)
        }
    }

    /**
     * Drops events which match the predicate.
     */
    fun Reject(predicate: (Event) -> Boolean) = Accept { !predicate(it) }

    /**
     * Only passes events of type T (including those wrapped in a MetadataEvent) which match the predicate.
     */
    @JvmName("AcceptType")
    inline fun <reified T : Event> Accept(crossinline predicate: (T) -> Boolean = { true }) =
        Accept { it.unwrapped().let { event -> event is T && predicate(event) } }

    /**
     * Drops events of type T (including those wrapped in a MetadataEvent) which match the predicate.
     */
    @JvmName("RejectType")
    inline fun <reified T : Event> Reject(crossinline predicate: (T) -> Boolean = { true }) =
        Reject { it.unwrapped().let { event -> event is T && predicate(event) } }

    /**
     * Adds Zipkin traces metadata to the event.
     */
    fun AddZipkinTraces(storage: ZipkinTracesStorage = ZipkinTracesStorage.THREAD_LOCAL) = EventFilter { next ->
        {
            next(it + ("traces" to storage.forCurrentThread()))
        }
    }
}

@PublishedApi
internal fun Event.unwrapped(): Event = if (this is MetadataEvent) event.unwrapped() else this
