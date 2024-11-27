package org.http4k.tracing.renderer

import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer

class MarkdownDocumentTest : TraceRendererContract(
    "foobar", "MD", MarkdownDocument(
        MarkdownTraceDepthTable,
        MarkdownTraceStepCountsTable,
        MermaidSequenceDiagram,
        MermaidInteractionDiagram,
        TraceRenderer { scenarioName, steps ->
            TraceRender(scenarioName, "TXT", steps.joinToString("\n"))
        }
    )
)
