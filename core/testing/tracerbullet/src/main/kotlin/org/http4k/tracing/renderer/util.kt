package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.Trace

/**
 * Returns all distinct actors in the list of Traces, so that they appear in chronological
 * order as to their position in the overall flow.
 */
fun List<Trace>.chronologicalActors(): List<Actor> {
    val origins = map { it.origin } + flatMap { it.children.flatMap(Trace::origins) }
    val targets = map { it.target } + flatMap { it.children.flatMap(Trace::targets) }
    return (origins + targets).distinct()
}

private fun Trace.origins(): List<Actor> = listOf(origin) + children.flatMap(Trace::origins)
private fun Trace.targets(): List<Actor> = listOf(target) + children.flatMap(Trace::targets)

internal fun String.identifier() = filter { it.isLetterOrDigit() }
