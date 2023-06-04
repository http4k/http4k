package org.http4k.tracing

import org.http4k.events.Event
import org.http4k.tracing.ActorType.Database
import java.time.Instant

data class DbEvent(val serviceIdentifier: String, val myTime: Instant) : Event

class DbTracer(private val actorFrom: ActorResolver) : Tracer {
    override fun invoke(node: EventNode, tracer: Tracer) = node
        .takeIf { it.event.event is DbEvent }
        ?.let {
            BiDirectional(
                actorFrom(it.event),
                Actor("db", Database),
                (it.event.event as DbEvent).serviceIdentifier,
                emptyList()
            )
        }
        ?.let(::listOf)
        ?: emptyList()
}
