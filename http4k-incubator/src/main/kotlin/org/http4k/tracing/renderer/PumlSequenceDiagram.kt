package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType.Database
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

object PumlSequenceDiagram : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val actors = steps.filterIsInstance<Trace>().chronologicalActors()

        return TraceRender(
            "$scenarioName - Sequence",
            "PUML",
            """@startuml
            |title $scenarioName
            |${actors.toActor().joinToString("\n")}
            |${
                steps.joinToString("\n") {
                    when (it) {
                        is RequestResponse -> it.asPumlSequenceDiagram()
                        is BiDirectional -> it.asPumlSequenceDiagram()
                        is FireAndForget -> it.asPumlSequenceDiagram()
                        is StartInteraction -> it.asPumlSequenceDiagram()
                        is StartRendering, is StopRendering -> ""
                    }
                }
            }
    @enduml""".trimMargin())
    }

    private fun Iterable<Actor>.toActor() =
        fold(emptyList<String>()) { acc, next ->
            val nextVal = when (next.type) {
                Database -> "database"
                else -> "participant"
            } + " \"${next.name}\""
            if (acc.contains(nextVal)) acc else acc + nextVal
        }

    private fun Trace.asPumlSequenceDiagram() = when (this) {
        is RequestResponse -> asPumlSequenceDiagram()
        is BiDirectional -> asPumlSequenceDiagram()
        is FireAndForget -> asPumlSequenceDiagram()
    }

    private fun RequestResponse.asPumlSequenceDiagram(): String = """
           |"${origin.name}" -> "${target.name}": $request
           |activate "${target.name}"
           |${children.joinToString("\n") { it.asPumlSequenceDiagram() }}
           |"${target.name}" -> "${origin.name}": $response
           |deactivate "${target.name}"
            """.trimMargin()

    private fun BiDirectional.asPumlSequenceDiagram(): String = """
           |"${origin.name}" <-> "${target.name}": $request
            """.trimMargin()

    private fun FireAndForget.asPumlSequenceDiagram(): String = """
           |"${origin.name}" -> "${target.name}": $request
            """.trimMargin()

    private fun StartInteraction.asPumlSequenceDiagram(): String = """
        
        note over "$origin" : "$origin" $interactionName
        """.trimIndent()
}

