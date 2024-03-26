package org.http4k.tracing

import org.http4k.events.Event
import org.http4k.events.HttpEvent
import org.http4k.events.MetadataEvent
import org.http4k.events.plus
import org.http4k.filter.TraceId
import org.http4k.tracing.CollectEvents.Collect
import org.http4k.tracing.CollectEvents.Drop
import org.http4k.tracing.tracer.TreeWalker

/**
 * Entry--point for creating Trace from a list of MetadataEvents. Provide a Tracer for each of the
 * implementations that you want to support.
 */
class TracerBullet(private val tracers: List<Tracer>) {
    constructor(vararg tracers: Tracer) : this(tracers.toList())

    operator fun invoke(events: List<Event>): List<Trace> =
        events.filterIsInstance<MetadataEvent>().removeUnrenderedEvents().buildTree()
            .flatMap { event -> tracers.flatMap { it(event, Tracer.TreeWalker(tracers)) } }
}

internal fun List<MetadataEvent>.buildTree(): List<EventNode> {
    val eventsByParent = groupBy { it.traces()?.parentSpanId }

    val rootEvents = filter { event ->
        eventsByParent.none { it.value.any { it.traces()?.spanId == event.traces()?.parentSpanId } }
    }

    return rootEvents.map { it.asEventNode(eventsByParent) }
}

private fun MetadataEvent.asEventNode(eventsByParent: Map<TraceId?, List<MetadataEvent>>): EventNode {
    val updated = (when (event) {
        is HttpEvent.Outgoing ->
            when (val incoming = eventsByParent[traces()?.traceId]?.firstOrNull(::matchingIncoming)) {
                null -> this
                else -> this + ("target" to incoming)
            }

        else -> this
    }) as MetadataEvent

    return EventNode(updated, createEventNodes(eventsByParent))
}

private fun MetadataEvent.matchingIncoming(candidate: MetadataEvent): Boolean =
    candidate.event is HttpEvent.Incoming && candidate.metadata["traces"] == metadata["traces"]

private fun MetadataEvent.createEventNodes(eventsByParent: Map<TraceId?, List<MetadataEvent>>): List<EventNode> =
    eventsByParent[traces()?.spanId]?.map { it.asEventNode(eventsByParent) } ?: emptyList()

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
