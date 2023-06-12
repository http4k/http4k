package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType.Database
import org.http4k.tracing.ActorType.Human
import org.http4k.tracing.Trace
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep

object PumlInteractionFlowDiagram : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {

        val traces = steps.filterIsInstance<Trace>()

        val relations = traces
            .flatMapIndexed { i, it -> it.relations("${i + 1}") }
            .toSet()

        return TraceRender(
            "$scenarioName - Flow",
            "PUML",
            """@startuml
title $scenarioName

!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml


${traces.chronologicalActors().toPumlActor().joinToString("\n")}    
${relations.joinToString("\n") { "Rel_D(${it.origin.identifier()}, ${it.target.identifier()}, \"${it.interaction}\")" }}    
@enduml""".trimMargin()

        )
    }

    private fun Iterable<Actor>.toPumlActor() =
        fold(emptyList<String>()) { acc, it ->
            val nextVal = when (it.type) {
                Database -> "ContainerDb"
                Human -> "Person"
                else -> "Container"
            } + "(${it.name.identifier()}, \"${it.name}\")"

            if (acc.contains(nextVal)) acc else acc + nextVal
        }

    private fun Trace.relations(prefix: String): List<Call> =
        listOf(
            Call(
                origin.name,
                target.name,
                "$prefix $request"
            )
        ) + children.flatMapIndexed { i, it -> it.relations("$prefix.${i + 1}") }

    private data class Call(val origin: String, val target: String, val interaction: String)
}
