package org.http4k.routing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.LLMTools
import org.http4k.ai.llm.tools.ToolHandler
import org.http4k.ai.llm.tools.ToolRequest

class RoutingToolHandler(private val tools: List<Pair<LLMTool, ToolHandler>>) : LLMTools,
    Iterable<Pair<LLMTool, ToolHandler>> by tools {
    private val byName = tools.associate { it.first.name to it.second }

    override fun list() = Success(tools.map { it.first })

    override fun invoke(request: ToolRequest) = byName[request.name]
        ?.let { it(request) }
        ?: Failure(LLMError.NotFound)
}
