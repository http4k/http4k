package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.model.Message
import java.io.PrintStream

fun interface Chat {
    operator fun invoke(request: ChatRequest): LLMResult<ChatResponse>

    companion object
}
