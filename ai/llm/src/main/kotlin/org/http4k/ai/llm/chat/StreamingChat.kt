package org.http4k.ai.llm.chat

import org.http4k.ai.llm.LLMResult

fun interface StreamingChat {
    operator fun invoke(request: ChatRequest): Sequence<LLMResult<ChatResponse>>
}
