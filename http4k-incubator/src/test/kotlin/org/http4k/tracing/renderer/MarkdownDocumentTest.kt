package org.http4k.tracing.renderer

class MarkdownDocumentTest : TraceRendererContract(
    "foobar", "MD", MarkdownDocument(
        MarkdownTraceDepthTable, MarkdownTraceStepCountsTable, MermaidSequenceDiagram
    )
)
