package org.http4k.ai.llm.tools

import org.http4k.ai.llm.LLMResult

fun interface ToolHandler {
    operator fun invoke(request: ToolRequest): LLMResult<ToolResponse>
}
