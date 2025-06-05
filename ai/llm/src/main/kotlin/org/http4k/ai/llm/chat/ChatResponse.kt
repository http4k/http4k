package org.http4k.ai.llm.chat

import org.http4k.ai.llm.model.Message
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.TokenUsage


data class ChatResponse(val message: Message.Assistant, val metadata: Metadata) {
    data class Metadata(
        val id: ResponseId,
        val model: ModelName,
        val usage: TokenUsage? = null,
        val stopReason: StopReason? = null
    )
}
