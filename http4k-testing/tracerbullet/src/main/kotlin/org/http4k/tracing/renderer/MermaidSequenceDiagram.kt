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

        val stepsRendered = steps.joinToString("\n    ") {
            when (it) {
                is RequestResponse -> it.asMermaidSequenceDiagram()
                is BiDirectional -> it.asMermaidSequenceDiagram()
                is FireAndForget -> it.asMermaidSequenceDiagram()
                is StartInteraction -> it.asMermaidSequenceDiagram()
                is StartRendering, is StopRendering -> ""
            }
        }
        val actorsRendered = actors.toMermaidActor().joinToString("\n    ")
        return TraceRender(
            "$scenarioName - Sequence",
            "MMD",
            listOf(
                "sequenceDiagram",
                "title $scenarioName - Sequence",
                actorsRendered,
                stepsRendered,
            ).joinToString("\n    ")
        )
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

    private fun RequestResponse.asMermaidSequenceDiagram(): String {
        val header = "${origin.safeName()} ->> ${target.safeName()}: $request"
        val activate = "activate ${target.safeName()}"
        val children = renderChildren()

        val deactivate = "deactivate ${target.safeName()}"

        val post = "${target.safeName()} ->> ${origin.safeName()}: $response"

        return listOfNotNull(header, activate, children, post, deactivate).joinToString("\n    ") { it.trim() }
    }

    private fun Trace.renderChildren() = children
        .joinToString("\n    ") { it.asMermaidSequenceDiagram() }
        .takeIf { it.isNotEmpty() }
        ?.let { "$it\n    " }

    private fun BiDirectional.asMermaidSequenceDiagram(): String {
        val pre = "${origin.safeName()} ->> ${target.safeName()}: $request".trim()
        val children = renderChildren()?.trim()
        val post = "${target.safeName()} ->> ${origin.safeName()}: "
        return listOfNotNull(pre, children, post).joinToString("\n    ")
    }

    private fun FireAndForget.asMermaidSequenceDiagram(): String {
        val pre = "${origin.safeName()} -) ${target.safeName()}: $request"
        val children = renderChildren()

        return listOfNotNull(pre, children).joinToString("\n    ") { it.trim() }
    }

    private fun StartInteraction.asMermaidSequenceDiagram(): String = """note over $origin: $interactionName"""
}

private fun Actor.safeName() = name.map { if (it.isWhitespace() || it.isLetterOrDigit()) it else '_' }.joinToString("")
