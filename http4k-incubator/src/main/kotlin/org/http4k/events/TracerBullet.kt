package org.http4k.events

import org.http4k.filter.ZipkinTraces

/**
 * Entry--point for creating TraceTrees from a list of MetadataEvents. Provide a Tracer for each of the
 * implementations that you want to support.
 */
class TracerBullet(private vararg val tracers: Tracer<*>) {

    operator fun invoke(events: List<Event>): List<TraceTree> {
        val metadataEvents = events.filterIsInstance<MetadataEvent>()

        return metadataEvents
            .filter { it.traces().parentSpanId == null }
            .flatMap { event -> tracers.flatMap { it(event, metadataEvents - event, uberTracer) } }
    }

    private val uberTracer = object : Tracer<TraceTree> {
        override operator fun invoke(parent: MetadataEvent, rest: List<MetadataEvent>, tracer: Tracer<TraceTree>) =
            tracers.flatMap { it(parent, rest - parent, this) }
    }

    private fun MetadataEvent.traces() = (metadata["traces"] as ZipkinTraces)

}
