package org.http4k.tracing

interface TraceLoader {
    fun load(): Iterable<ScenarioTraces>
    companion object
}
