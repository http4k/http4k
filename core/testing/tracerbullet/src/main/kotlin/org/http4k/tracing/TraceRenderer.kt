package org.http4k.tracing

fun interface TraceRenderer {
    fun render(scenarioName: String, steps: List<TraceStep>): TraceRender
}
