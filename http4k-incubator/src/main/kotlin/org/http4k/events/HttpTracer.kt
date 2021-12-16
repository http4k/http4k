package org.http4k.events

import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.filter.ZipkinTraces

/**
 * Traces HTTP call stacks for standard HttpEvents.
 * The important thing here is to supply the way of getting the
 * outbound host (or service) name out of the parent and child metadata events
 * - we can do this with an EventFilter.
 */
fun HttpTracer(
    childHostName: (MetadataEvent) -> String,
    parentHostName: (MetadataEvent) -> String = { (it.event as HttpEvent.Outgoing).uri.host },
) = object : Tracer<HttpTraceTree> {
    override operator fun invoke(
        parent: MetadataEvent,
        rest: List<MetadataEvent>,
        tracer: Tracer<TraceTree>
    ) = parent.takeIf { it.event is HttpEvent.Outgoing }
        ?.let { listOf(it.toTraceTree(rest - it, tracer)) } ?: emptyList()

    private fun MetadataEvent.toTraceTree(rest: List<MetadataEvent>, tracer: Tracer<TraceTree>): HttpTraceTree {
        val parentEvent = event as HttpEvent.Outgoing
        return HttpTraceTree(
            childHostName(this),
            parentEvent.uri, parentEvent.method, parentEvent.status,
            rest
                .filter { traces().spanId == it.traces().parentSpanId }
                .filter { parentHostName(this) == childHostName(it) }
                .flatMap { tracer(it, rest - it, tracer) })
    }

    private fun MetadataEvent.traces() = (metadata["traces"] as ZipkinTraces)
}

data class HttpTraceTree(
    override val origin: String,
    val uri: Uri,
    val method: Method,
    val status: Status,
    override val children: List<TraceTree>
) : TraceTree
