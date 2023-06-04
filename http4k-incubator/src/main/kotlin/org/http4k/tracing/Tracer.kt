package org.http4k.tracing

/**
 * Implement this to define custom Trace Event types - eg. writing to a database or sending a message
 */
fun interface Tracer {
    operator fun invoke(node: EventNode, tracer: Tracer): List<Trace>

    companion object
}

