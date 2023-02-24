package org.http4k.tracing

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.tracing.ActorType.Database
import java.time.Instant

data class MyCustomEvent(val serviceIdentifier: String, val myTime: Instant) : Event

class MyCustomTracer(private val actorFrom: ActorResolver) : Tracer {
    override fun invoke(parent: MetadataEvent, rest: List<MetadataEvent>, tracer: Tracer) = parent
        .takeIf { it.event is MyCustomEvent }
        ?.let {
            BiDirectional(
                actorFrom(it),
                Actor("db", Database),
                (it.event as MyCustomEvent).serviceIdentifier,
                emptyList()
            )
        }
        ?.let { listOf(it) }
        ?: emptyList()
}
