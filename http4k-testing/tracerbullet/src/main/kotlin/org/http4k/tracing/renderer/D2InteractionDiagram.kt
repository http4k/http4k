package org.http4k.tracing.renderer

import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType.Database
import org.http4k.tracing.ActorType.Human
import org.http4k.tracing.ActorType.Queue
import org.http4k.tracing.Trace
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep

object D2InteractionDiagram : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender {
        val traces = steps.filterIsInstance<Trace>()

        val relations = traces.flatMap { it.relations() }.toSet()

        return TraceRender(
            "$scenarioName - Interactions",
            "D2",
            """
title: |md
# $scenarioName - Interactions
| {near: top-center}

${traces.chronologicalActors().toD2Actor().joinToString("\n")}    
${relations.joinToString("\n") { "${it.origin.safe()} -> ${it.target.safe()}" }}    
"""
        )
    }

    private fun Iterable<Actor>.toD2Actor() =
        fold(emptyList<String>()) { acc, it ->
            val nextVal = when (it.type) {
                Database -> """
                    ${it.safeName()}: {
                         shape: cylinder
                    }
                """.trimIndent()

                Human -> """
                    ${it.safeName()}: {
                        shape: person
                    }
                """.trimIndent()

                Queue -> """
                    ${it.safeName()}: {
                        shape: queue
                    }
                """.trimIndent()

                else -> """
                    ${it.safeName()}: {
                        shape: square
                    }
                """.trimIndent()
            }

            if (acc.contains(nextVal)) acc else acc + nextVal
        }

    private fun Trace.relations(): List<Call> =
        listOf(Call(origin.name, target.name)) + children.flatMap { it.relations() }

    private data class Call(val origin: String, val target: String)
}

private fun Actor.safeName() = name.safe()

private fun String.safe() = replace('.', '_')
