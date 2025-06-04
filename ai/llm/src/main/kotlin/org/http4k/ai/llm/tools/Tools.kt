package org.http4k.ai.llm.tools

import org.http4k.ai.llm.LLMResult

interface Tools : ToolHandler {
    fun list(): LLMResult<List<LLMTool>>
}
