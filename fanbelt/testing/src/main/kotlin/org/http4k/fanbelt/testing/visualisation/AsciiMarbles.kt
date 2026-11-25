package org.http4k.fanbelt.testing.visualisation

import org.http4k.tracing.TraceRender

/**
 * Renders a marble timeline as plain text, for approval tests.
 */
class AsciiMarbles : MarbleRenderer {
    override fun render(name: String, timeline: Timeline): TraceRender {
        val width = timeline.labelWidth + 2
        val rows = timeline.lanes.joinToString("\n") { lane ->
            val glyphs = lane.marbles.associate { it.column to it.kind.glyph }
            lane.name.padEnd(width) + (0 until timeline.columns).joinToString("") { "-${glyphs[it] ?: '-'}" }
        }
        return TraceRender(name, "MARBLES", rows)
    }
}
