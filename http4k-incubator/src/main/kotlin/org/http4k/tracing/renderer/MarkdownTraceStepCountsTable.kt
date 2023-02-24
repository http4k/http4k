package org.http4k.tracing.renderer

import org.http4k.tracing.Trace
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep

object MarkdownTraceStepCountsTable : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val body = steps.filterIsInstance<Trace>()
            .map { it to it.counts() }
            .sortedByDescending { it.second }
            .joinToString("\n") {
                "|\t${it.first.origin.name}\t|\t${it.first.target.name}\t|\t${it.first.request}\t|\t${it.second}\t|"
            }

        val header = """
## $scenarioName - Trace Step Counts

| Origin | Target | Request |  Steps  |
|:------:|:------:|:-------:|:-------:|
"""
        return TraceRender("$scenarioName - Trace Step Counts", "MD", header + body)
    }

    private fun Trace.counts(): Int = 1 + children.fold(0) { acc, next -> acc + next.counts()}
}
