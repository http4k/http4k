package org.http4k.ai.chat

import org.http4k.ai.AiResult
import org.http4k.ai.model.Message

interface ChatMemory {
    fun messages(memoryId: ChatMemoryId): AiResult<List<Message>>

    fun add(memoryId: ChatMemoryId, messages: List<Message>): AiResult<Unit>

    fun update(memoryId: ChatMemoryId, messages: List<Message>): AiResult<Unit>

    fun delete(memoryId: ChatMemoryId): AiResult<Unit>
}
