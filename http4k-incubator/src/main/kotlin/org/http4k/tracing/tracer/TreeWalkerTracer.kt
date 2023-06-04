package org.http4k.tracing.tracer

import org.http4k.tracing.EventNode
import org.http4k.tracing.Tracer

fun Tracer.Companion.TreeWalker(tracers: List<Tracer>) = object : Tracer {
    override operator fun invoke(node: EventNode, tracer: Tracer) = tracers.flatMap { it(node, this) }
}
