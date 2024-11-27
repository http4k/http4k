package org.http4k.tracing.renderer

import org.http4k.core.Status
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType
import org.http4k.tracing.ActorType.*
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
                Human -> "actor"
                System -> "participant"
                Queue -> "queue"
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
           |"${target.name}" ${response.toArrow()} "${origin.name}": ${response.toColour()} $response
           |deactivate "${target.name}"
            """.trimMargin()

    private fun String.toColour() = try {
        with(toStatus()) {
            when {
                successful -> "<color:DarkGreen>"
                redirection -> "<color:DarkBlue>"
                clientError -> "<color:DarkOrange>"
                serverError -> "<color:FireBrick>"
                else -> "<color:Black>"
            }
        }
    } catch (e: Exception) {
        "<color:Black>>"
    }

    private fun BiDirectional.asPumlSequenceDiagram(): String = (
        """
           |"${origin.name}" <-> "${target.name}": $request""" +
            if (children.isNotEmpty()) """|activate "${target.name}"
           |${children.joinToString("\n") { it.asPumlSequenceDiagram() }}
           |deactivate "${target.name}"""
            else ""
        ).trimMargin()

    private fun FireAndForget.asPumlSequenceDiagram(): String = (
        """
           |"${origin.name}" -> "${target.name}": $request""" +
            if (children.isNotEmpty()) """
           |${children.joinToString("\n") { it.asPumlSequenceDiagram() }}"""
            else ""
        ).trimMargin()

    private fun StartInteraction.asPumlSequenceDiagram(): String = """

    note over "$origin" : "$origin" $interactionName
    """.trimIndent()

    private fun String.toArrow(): String =
        try {
            with(toStatus()) {
                when {
                    successful -> "-[#DarkGreen]>"
                    redirection -> "-[#DarkBlue]>"
                    clientError -> "X-[#DarkOrange]>"
                    serverError -> "X-[#FireBrick]>"
                    else -> "-[#Black]>"
                }
            }
        } catch (e: Exception) {
            "-->"
        }

    private fun String.toStatus() = Status(
        split(" ").first()
            .filter(Char::isDigit)
            .toInt(), split(" ").last()
    )
}
