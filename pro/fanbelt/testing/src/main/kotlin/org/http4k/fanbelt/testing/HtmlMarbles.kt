/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.tracing.TraceRender

class HtmlMarbles : MarbleRenderer {
    override fun render(name: String, timeline: Timeline): TraceRender {
        val labels = timeline.labelWidth * CHAR + 24
        val width = labels + (timeline.columns + 1) * COLUMN
        val height = timeline.lanes.size * ROW + 8
        val rows = timeline.lanes.mapIndexed { row, lane -> lane(lane, labels, width, row * ROW + ROW / 2 + 4) }.joinToString("\n")
        val title = name.escaped()
        return TraceRender(
            name, "HTML", """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>$title</title>
<style>
:root { --ink: #1d1d1f; --line: #c9c9cf; --page: #ffffff; --hollow: #8a8a94; --changes: #2f9e44; --rejected: #e03131; --denied: #9c36b5; --happened: #339af0; }
@media (prefers-color-scheme: dark) { :root { --ink: #ececf1; --line: #4a4a52; --page: #18181b; --hollow: #9a9aa4; --changes: #40c057; --rejected: #fa5252; --denied: #cc5de8; --happened: #74c0fc; } }
body { background: var(--page); color: var(--ink); font: 14px ui-monospace, Menlo, monospace; margin: 24px; }
h1 { font-size: 16px; font-weight: 600; }
.name { fill: var(--ink); }
.lane { stroke: var(--line); stroke-width: 2; }
.glyph { fill: #ffffff; font-size: 13px; text-anchor: middle; dominant-baseline: central; }
.changes { fill: var(--changes); }
.rejected { fill: var(--rejected); }
.denied { fill: var(--denied); }
.happened { fill: var(--happened); }
.dropped { fill: none; stroke: var(--hollow); stroke-width: 2; }
</style>
</head>
<body>
<h1>$title</h1>
<svg width="$width" height="$height" viewBox="0 0 $width $height" role="img" aria-label="$title">
$rows
</svg>
</body>
</html>
"""
        )
    }

    private fun lane(lane: Lane, labels: Int, width: Int, y: Int): String {
        val marbles = lane.marbles.joinToString("") { marble(it, labels + (it.column + 1) * COLUMN, y) }
        return """<text class="name" x="0" y="$y" dominant-baseline="central">${lane.name.escaped()}</text>""" +
            """<line class="lane" x1="$labels" y1="$y" x2="$width" y2="$y"/>$marbles"""
    }

    private fun marble(marble: Marble, x: Int, y: Int): String {
        val circle = """<circle class="${marble.kind.name.lowercase()}" cx="$x" cy="$y" r="$RADIUS"/>"""
        return if (marble.kind.labelled) circle + """<text class="glyph" x="$x" y="$y">${marble.kind.glyph}</text>""" else circle
    }

    private fun String.escaped() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private companion object {
        const val ROW = 44
        const val COLUMN = 34
        const val RADIUS = 12
        const val CHAR = 8
    }
}
