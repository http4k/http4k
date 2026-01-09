package org.http4k.ai.llm.tools

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMError

fun LLMTools.Companion.InMemory(tools: Map<LLMTool, ToolHandler>) = object : LLMTools {
    override fun list() = Success(tools.keys.toList())

    override fun invoke(request: ToolRequest) = tools.toList()
        .firstOrNull { it.first.name == request.name }?.second
        ?.let { it(request) }
        ?: Failure(LLMError.NotFound)
}
