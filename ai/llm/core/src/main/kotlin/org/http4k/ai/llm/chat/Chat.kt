package org.http4k.ai.llm.chat

import org.http4k.ai.llm.LLMResult

fun interface Chat {
    operator fun invoke(request: ChatRequest): LLMResult<ChatResponse>

    companion object
}
