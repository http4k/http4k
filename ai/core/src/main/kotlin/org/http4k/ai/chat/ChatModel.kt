package org.http4k.ai.chat

import org.http4k.ai.AiResult
import org.http4k.ai.model.Message
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ModelParams
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.TokenUsage

typealias ChatModel = (ChatRequest) -> AiResult<ChatResponse>

typealias StreamingChatModel = (ChatRequest) -> Sequence<AiResult<ChatResponse>>

data class ChatRequest(val messages: List<Message>, val params: ModelParams? = null)

data class ChatResponse(val message: Message.Ai, val metadata: Metadata) {
    data class Metadata(
        val id: ResponseId,
        val modelName: ModelName,
        val tokenUsage: TokenUsage? = null,
        val finishReason: StopReason? = null
    )
}

