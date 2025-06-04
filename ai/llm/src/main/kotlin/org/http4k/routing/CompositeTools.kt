package org.http4k.routing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.llm.tools.Tools

/**
 * A composite of multiple [Tools] instances, which allows for invoking tools across multiple collections.
 *
 * @property toolCollections The iterable of [Tools] instances to be composed.
 */
class CompositeTools(private val toolCollections: Iterable<Tools>) : Tools {
    override fun list() = toolCollections.map { it.list() }.allValues().map { it.flatten() }

    override fun invoke(request: ToolRequest) = toolCollections
        .map { tools -> tools.list().map { it.map { it.name to tools } }.map { it.toList() } }
        .allValues()
        .map { it.flatten().toMap()[request.name] }
        .flatMap { if (it == null) Failure(LLMError.NotFound) else it(request) }
}
