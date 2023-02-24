package org.http4k.tracing

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.tracing.CollectEvents.Collect
import org.http4k.tracing.CollectEvents.Drop
import org.http4k.tracing.tracer.TreeWalker

/**
 * Entry--point for creating Trace from a list of MetadataEvents. Provide a Tracer for each of the
 * implementations that you want to support.
 */
class TracerBullet(private val tracers: List<Tracer>) {
    constructor(vararg tracers: Tracer) : this(tracers.toList())

    operator fun invoke(events: List<Event>): List<Trace> {
        val metadataEvents = events.filterIsInstance<MetadataEvent>().removeUnrenderedEvents()
        val uberTracer = Tracer.TreeWalker(tracers)

        return metadataEvents
            .filter { it.traces()?.let { it.parentSpanId == null } ?: false }
            .flatMap { event -> tracers.flatMap { it(event, metadataEvents - event, uberTracer) } }
    }
}

private enum class CollectEvents { Collect, Drop }

private fun List<MetadataEvent>.removeUnrenderedEvents(): List<MetadataEvent> {
    fun List<MetadataEvent>.andNext(collectEvents: CollectEvents) = this to collectEvents

    val collectElements = if (any { it.event == StartRendering }) Drop else Collect

    return fold(Pair(listOf<MetadataEvent>(), collectElements)) { acc, event ->
        when (acc.second) {
            Collect -> when (event.event) {
                StopRendering -> acc.first.andNext(Drop)
                else -> (acc.first + event).andNext(Collect)
            }

            Drop -> when (event.event) {
                StartRendering -> acc.first.andNext(Collect)
                else -> acc.first.andNext(Drop)
            }
        }
    }.first
}
