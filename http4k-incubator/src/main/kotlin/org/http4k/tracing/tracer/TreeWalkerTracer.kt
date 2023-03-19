package org.http4k.tracing.tracer

import org.http4k.events.MetadataEvent
import org.http4k.tracing.Tracer

fun Tracer.Companion.TreeWalker(tracers: List<Tracer>) = object : Tracer {
    override operator fun invoke(
        parent: MetadataEvent,
        rest: List<MetadataEvent>,
        tracer: Tracer
    ) = tracers.flatMap { it(parent, rest - parent, this) }
}