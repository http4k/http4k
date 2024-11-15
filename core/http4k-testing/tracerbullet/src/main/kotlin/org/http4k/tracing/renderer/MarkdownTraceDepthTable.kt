package org.http4k.tracing.renderer

import org.http4k.tracing.Trace
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep

object MarkdownTraceDepthTable : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val body = steps.filterIsInstance<Trace>()
            .map { it to it.maxDepth(1) }
            .sortedByDescending { it.second }
            .joinToString("\n") {
                "|\t${it.first.origin.name}\t|\t${it.first.target.name}\t|\t${it.first.request}\t|\t${it.second}\t|"
            }

        val header = """
## $scenarioName - Maximum Trace Depth

| Origin | Target | Request |  Max Depth  |
|:------:|:------:|:-------:|:-----------:|
"""
        return TraceRender("$scenarioName - Maximum Trace Depth", "MD", header + body)
    }

    private fun Trace.maxDepth(i: Int): Int = (listOf(i) + children.map { it.maxDepth(i + 1) }).max()
}
