package org.http4k.tracing

data class ScenarioTraces(val name: String, val traces: List<Trace>)

fun Iterable<ScenarioTraces>.combineTo(name: String) = ScenarioTraces(name, flatMap { it.traces })
