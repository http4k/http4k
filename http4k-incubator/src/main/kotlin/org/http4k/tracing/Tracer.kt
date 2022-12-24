package org.http4k.tracing

import org.http4k.events.MetadataEvent

/**
 * Implement this to define custom Trace Event types - eg. writing to a database or sending a message
 */
fun interface Tracer {
    operator fun invoke(parent: MetadataEvent, rest: List<MetadataEvent>, tracer: Tracer): List<Trace>

    companion object
}
