package org.http4k.tracing

interface TracePersistence {
    fun store(trace: ScenarioTraces)
    fun load(): Iterable<ScenarioTraces>

    companion object
}

