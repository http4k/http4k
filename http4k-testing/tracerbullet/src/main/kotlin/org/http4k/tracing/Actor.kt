package org.http4k.tracing

import org.http4k.events.MetadataEvent

data class Actor(val name: String, val type: ActorType)

/**
 * Implement this to resolve the type of Actor represented by the event in the system.
 */
fun interface ActorResolver : (MetadataEvent) -> Actor

enum class ActorType {
    Human, System, Database, Queue
}
