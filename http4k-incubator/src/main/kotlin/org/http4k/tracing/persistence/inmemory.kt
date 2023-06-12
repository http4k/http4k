package org.http4k.tracing.persistence

import org.http4k.core.Uri
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderPersistence

fun TracePersistence.Companion.InMemory() = object : TracePersistence {
    private val list = mutableListOf<ScenarioTraces>()
    override fun store(trace: ScenarioTraces) {
        list += trace
    }

    override fun load() = list
}

interface IterableTraceRenderPersistence : TraceRenderPersistence, Iterable<TraceRender>

fun TraceRenderPersistence.Companion.InMemory() = object : IterableTraceRenderPersistence {
    private val renders = mutableListOf<TraceRender>()

    override fun invoke(render: TraceRender): Uri? {
        renders += render
        return null
    }

    override fun iterator() = renders.iterator()
}
