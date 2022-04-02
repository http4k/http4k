package org.http4k.events

import org.http4k.filter.ZipkinTraces
import java.time.Instant

data class MyCustomEvent(val serviceIdentifier: String, val myTime: Instant) : Event

object MyCustomTracer : Tracer<MyTraceTree> {
    override fun invoke(parent: MetadataEvent, rest: List<MetadataEvent>, tracer: Tracer<TraceTree>) =
        parent.takeIf { it.event is MyCustomEvent }
            ?.let { listOf(it.toTraceTree(rest - it, tracer)) } ?: emptyList()

    private fun MetadataEvent.toTraceTree(rest: List<MetadataEvent>, tracer: Tracer<TraceTree>): MyTraceTree {
        val parentEvent = event as MyCustomEvent
        return MyTraceTree(
            service(this),
            parentEvent.myTime,
            rest
                .filter { traces().spanId == it.traces().parentSpanId }
                .filter { parentEvent.serviceIdentifier == service(it) }
                .flatMap { tracer(it, rest - it, tracer) })
    }

    private fun service(event: MetadataEvent) = event.metadata["service"].toString()
    private fun MetadataEvent.traces() = (metadata["traces"] as ZipkinTraces)
}

data class MyTraceTree(
    override val origin: String,
    val myTime: Instant,
    override val children: List<TraceTree>
) : TraceTree
