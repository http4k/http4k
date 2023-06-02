package org.http4k.tracing

import org.http4k.events.Event
import org.http4k.tracing.ActorType.Database
import java.time.Instant

data class MyCustomEvent(val serviceIdentifier: String, val myTime: Instant) : Event

class MyCustomTracer(private val actorFrom: ActorResolver) : Tracer {
    override fun invoke(node: EventNode, tracer: Tracer) = node
        .takeIf { it.event.event is MyCustomEvent }
        ?.let {
            BiDirectional(
                actorFrom(it.event),
                Actor("db", Database),
                (it.event.event as MyCustomEvent).serviceIdentifier,
                emptyList()
            )
        }
        ?.let { listOf(it) }
        ?: emptyList()
}
