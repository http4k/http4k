package org.http4k.tracing.persistence

import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderPersistence

interface IterableTraceRenderPersistence : TraceRenderPersistence, Iterable<TraceRender>

fun TraceRenderPersistence.Companion.InMemory() = object : IterableTraceRenderPersistence {
    private val renders = mutableListOf<TraceRender>()

    override fun invoke(render: TraceRender) {
        renders += render
    }

    override fun iterator() = renders.iterator()
}
