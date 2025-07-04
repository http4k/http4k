package org.http4k.ai.llm.memory

import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.model.Message

interface LLMMemory {
    fun create(messages: List<Message> = emptyList()): LLMResult<LLMMemoryId>

    fun read(memoryId: LLMMemoryId): LLMResult<List<Message>>

    fun add(memoryId: LLMMemoryId, messages: List<Message>): LLMResult<LLMMemoryId>

    fun update(memoryId: LLMMemoryId, messages: List<Message>): LLMResult<LLMMemoryId>

    fun delete(memoryId: LLMMemoryId): LLMResult<LLMMemoryId>

    companion object
}
