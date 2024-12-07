package org.http4k.tracing.tracer

import org.http4k.events.MetadataEvent
import org.http4k.events.ProtocolEvent.Outgoing
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorResolver
import org.http4k.tracing.ActorType.System
import org.http4k.tracing.EventNode
import org.http4k.tracing.RequestResponse
import org.http4k.tracing.Trace
import org.http4k.tracing.Tracer
import org.http4k.tracing.X_HTTP4K_INCOMING_EVENT

fun HttpTracer(actorFrom: ActorResolver) = Tracer { eventNode, tracer ->
    eventNode
        .takeIf { it.event.event is Outgoing }
        ?.toTrace(actorFrom, tracer)
        ?.let(::listOf) ?: emptyList()
}

private fun EventNode.toTrace(actorFrom: ActorResolver, tracer: Tracer): Trace {
    val parentEvent = event.event as Outgoing

    return RequestResponse(
        actorFrom(event),
        (event.metadata[X_HTTP4K_INCOMING_EVENT] as? MetadataEvent)
            ?.let(actorFrom)
            ?: Actor(parentEvent.uri.host, System),
        parentEvent.method.name + " " + parentEvent.xUriTemplate,
        parentEvent.status.toString(),
        children.flatMap { tracer(it, tracer) }
    )
}

