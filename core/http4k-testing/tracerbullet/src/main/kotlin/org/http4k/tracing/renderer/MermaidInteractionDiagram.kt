package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType.Database
import org.http4k.tracing.ActorType.Human
import org.http4k.tracing.Trace
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep

object MermaidInteractionDiagram : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val traces = steps.filterIsInstance<Trace>()

        val relations = traces.flatMap { it.relations() }.toSet()

        return TraceRender(
            "$scenarioName - Interactions",
            "MMD",
            """C4Context
title $scenarioName

${traces.chronologicalActors().toMermaidActor().joinToString("\n")}    
${relations.joinToString("\n") { "Rel_D(${it.origin.identifier()}, ${it.target.identifier()}, \" \") " }}    
""".trimMargin()
        )
    }

    private fun Iterable<Actor>.toMermaidActor() =
        fold(emptyList<String>()) { acc, it ->
            val nextVal = when (it.type) {
                Database -> "ContainerDb"
                Human -> "Person"
                else -> "System"
            } + "(${it.name.identifier()}, \"${it.name}\")"

            if (acc.contains(nextVal)) acc else acc + nextVal
        }

    private fun Trace.relations(): List<Call> =
        listOf(Call(origin.name, target.name)) + children.flatMap { it.relations() }

    private data class Call(val origin: String, val target: String)
}
