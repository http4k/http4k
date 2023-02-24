package org.http4k.tracing.tracer

import org.http4k.events.HttpEvent
import org.http4k.events.MetadataEvent
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorResolver
import org.http4k.tracing.ActorType.System
import org.http4k.tracing.RequestResponse
import org.http4k.tracing.Trace
import org.http4k.tracing.Tracer
import org.http4k.tracing.traces

fun HttpTracer(actorFrom: ActorResolver) = Tracer { parent, rest, tracer ->
    parent
        .takeIf { it.event is HttpEvent.Outgoing }
        ?.let { it.toTrace(actorFrom, rest - it, tracer) }
        ?.let { listOf(it) } ?: emptyList()
}

private fun MetadataEvent.toTrace(actorFrom: ActorResolver, rest: List<MetadataEvent>, tracer: Tracer): Trace {
    val parentEvent = event as HttpEvent.Outgoing

    return RequestResponse(
        actorFrom(this),
        Actor(parentEvent.uri.host, System),
        parentEvent.method.name + " " + parentEvent.xUriTemplate,
        parentEvent.status.toString(),
        rest
            .filter { it.traces() != null && traces()?.spanId == it.traces()?.parentSpanId }
            .filter { parentEvent.uri.host == actorFrom(it).name }
            .flatMap { tracer(it, rest - it, tracer) }
    )
}

