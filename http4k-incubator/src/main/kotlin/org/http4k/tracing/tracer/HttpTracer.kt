package org.http4k.tracing.tracer

import org.http4k.events.HttpEvent
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorResolver
import org.http4k.tracing.ActorType.System
import org.http4k.tracing.EventNode
import org.http4k.tracing.RequestResponse
import org.http4k.tracing.Trace
import org.http4k.tracing.Tracer
import org.http4k.tracing.traces

fun HttpTracer(actorFrom: ActorResolver) = Tracer { parent, tracer ->
    parent
        .takeIf { it.event.event is HttpEvent.Outgoing }
        ?.toTrace(actorFrom, tracer)
        ?.let { listOf(it) } ?: emptyList()
}

private fun EventNode.toTrace(actorFrom: ActorResolver, tracer: Tracer): Trace {
    val parentEvent = event.event as HttpEvent.Outgoing

    return RequestResponse(
        actorFrom(event),
        Actor(parentEvent.uri.host, System),
        parentEvent.method.name + " " + parentEvent.xUriTemplate,
        parentEvent.status.toString(),
        children
            .filter { it.event.traces() != null && event.traces()?.spanId == it.event.traces()?.parentSpanId }
            .filter { parentEvent.uri.host == actorFrom(it.event).name }
            .flatMap { tracer(it, tracer) }
    )
}

