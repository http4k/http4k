/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.tracing.TraceRender

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
