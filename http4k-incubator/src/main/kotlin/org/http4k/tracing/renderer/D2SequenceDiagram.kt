package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.BiDirectional
import org.http4k.tracing.FireAndForget
import org.http4k.tracing.RequestResponse
import org.http4k.tracing.StartInteraction
import org.http4k.tracing.StartRendering
import org.http4k.tracing.StopRendering
import org.http4k.tracing.Trace
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep

object D2SequenceDiagram : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val actors = steps.filterIsInstance<Trace>().chronologicalActors()

        return TraceRender(
            "$scenarioName - Sequence",
            "D2",
            """
title: |md
# $scenarioName - Sequence
| {near: top-center}
shape: sequence_diagram
    ${actors.toD2Actors().joinToString("; ")}
${
                steps.joinToString("\n\t") {
                    when (it) {
                        is RequestResponse -> it.asD2SequenceDiagram()
                        is BiDirectional -> it.asD2SequenceDiagram()
                        is FireAndForget -> it.asD2SequenceDiagram()
                        is StartInteraction -> it.asD2SequenceDiagram()
                        is StartRendering, is StopRendering -> ""
                    }
                }
            }""")
    }

    private fun Trace.asD2SequenceDiagram() = when (this) {
        is RequestResponse -> asD2SequenceDiagram()
        is BiDirectional -> asD2SequenceDiagram()
        is FireAndForget -> asD2SequenceDiagram()
    }

    private fun Iterable<Actor>.toD2Actors() =
        map { it.name }.distinct()

    private fun RequestResponse.asD2SequenceDiagram(): String = """
    ${origin.name} -> ${target.name}: $request
    ${children.joinToString("\n\t") { it.asD2SequenceDiagram() }}
    ${target.name} -> ${origin.name}: $response
    """

    private fun BiDirectional.asD2SequenceDiagram(): String = """
    ${origin.name} <-> ${target.name}: $request
    ${children.joinToString("\n\t") { it.asD2SequenceDiagram() }}
    """

    private fun FireAndForget.asD2SequenceDiagram(): String = """
    ${origin.name} -> ${target.name}: $request
    ${children.joinToString("\n\t") { it.asD2SequenceDiagram() }}
    """

    private fun StartInteraction.asD2SequenceDiagram(): String = """
        note over $origin: $interactionName
        """
}

private fun Actor.safeName() = name
