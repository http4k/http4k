package org.http4k.ai.llm.tools

import org.http4k.ai.llm.LLMResult

/**
 * Tools that can be presented to and used by LLM to perform actions or retrieve information.
 */
interface LLMTools : ToolHandler {
    fun list(): LLMResult<List<LLMTool>>
}
