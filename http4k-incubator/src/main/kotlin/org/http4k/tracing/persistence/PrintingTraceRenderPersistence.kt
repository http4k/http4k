package org.http4k.tracing.persistence

import org.http4k.tracing.TraceRenderPersistence

/**
 * Prints the render to the console
 */
fun TraceRenderPersistence.Companion.Printing() = TraceRenderPersistence {
    println(it.title)
    println(it.content)
}
