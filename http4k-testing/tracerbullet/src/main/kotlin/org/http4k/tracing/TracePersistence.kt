package org.http4k.tracing

interface TracePersistence : TraceLoader {
    fun store(trace: ScenarioTraces)
    companion object
}

