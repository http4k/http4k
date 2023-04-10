package org.http4k.tracing

import org.http4k.events.MetadataEvent

/**
 * Implement this to define custom Tracer types - eg. writing to a database or sending a message
 */
fun interface Tracer {
    operator fun invoke(node: EventNode, tracer: Tracer): List<Trace>

    companion object
}

data class EventNode(val event: MetadataEvent, val children: List<EventNode>)
