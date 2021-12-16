package org.http4k.events

/**
 * Implement this to define custom Trace Event types - eg. writing to a database or sending a message
 */
interface Tracer<T : TraceTree> {
    operator fun invoke(parent: MetadataEvent, rest: List<MetadataEvent>, tracer: Tracer<TraceTree>): List<T>
}

/**
 * Models the overall tree structure.
 */
interface TraceTree {
    val origin: String
    val children: List<TraceTree>
}

