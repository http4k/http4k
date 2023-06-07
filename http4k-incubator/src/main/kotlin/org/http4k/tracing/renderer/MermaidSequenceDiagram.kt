package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType.Human
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

object MermaidSequenceDiagram : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val actors = steps.filterIsInstance<Trace>().chronologicalActors()

        return TraceRender(
            "$scenarioName - Sequence",
            "MMD",
            """sequenceDiagram
    title $scenarioName - Sequence
    ${actors.toMermaidActor().joinToString("\n\t")}
${
                steps.joinToString("\n\t") {
                    when (it) {
                        is RequestResponse -> it.asMermaidSequenceDiagram()
                        is BiDirectional -> it.asMermaidSequenceDiagram()
                        is FireAndForget -> it.asMermaidSequenceDiagram()
                        is StartInteraction -> it.asMermaidSequenceDiagram()
                        is StartRendering, is StopRendering -> ""
                    }
                }
            }""")
    }

    private fun Trace.asMermaidSequenceDiagram() = when (this) {
        is RequestResponse -> asMermaidSequenceDiagram()
        is BiDirectional -> asMermaidSequenceDiagram()
        is FireAndForget -> asMermaidSequenceDiagram()
    }

    private fun Iterable<Actor>.toMermaidActor() =
        fold(emptyList<String>()) { acc, next ->
            val nextVal = when (next.type) {
                Human -> "actor"
                else -> "participant"
            } + " ${next.safeName()}"
            if (acc.contains(nextVal)) acc else acc + nextVal
        }

    private fun RequestResponse.asMermaidSequenceDiagram(): String = """
    ${origin.safeName()}->>${target.safeName()}: $request
    activate ${target.safeName()}
    ${children.joinToString("\n\t") { it.asMermaidSequenceDiagram() }}
    ${target.safeName()}->>${origin.safeName()}: $response
    deactivate ${target.safeName()}
    """

    private fun BiDirectional.asMermaidSequenceDiagram(): String = """
    ${origin.safeName()}->>${target.safeName()}: $request
    ${children.joinToString("\n\t") { it.asMermaidSequenceDiagram() }}
    ${target.safeName()}->>${origin.safeName()}: 
    """

    private fun FireAndForget.asMermaidSequenceDiagram(): String = """
    ${origin.safeName()}-)${target.safeName()}: $request
    ${children.joinToString("\n\t") { it.asMermaidSequenceDiagram() }}
    ${children.joinToString("\n\t") { it.asMermaidSequenceDiagram() }}
    """

    private fun StartInteraction.asMermaidSequenceDiagram(): String = """
        
        note over $origin: $interactionName
        """
}

private fun Actor.safeName() = name.map { if (it.isWhitespace() || it.isLetterOrDigit()) it else '_' }.joinToString("")
