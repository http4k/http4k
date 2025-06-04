package org.http4k.ai.llm.memory

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.LLMError.Internal
import org.http4k.ai.llm.LLMError.NotFound
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.model.Message
import java.util.UUID

fun LLMMemory.Companion.InMemory() = object : LLMMemory {
    private val memoryStore = mutableMapOf<LLMMemoryId, MutableList<Message>>()

    override fun create(messages: List<Message>) =
        update(LLMMemoryId.of(UUID(0, memoryStore.size.toLong())), messages)

    override fun read(memoryId: LLMMemoryId) = memoryStore[memoryId]
        ?.let { Success(it) }
        ?: Failure(NotFound)

    override fun add(memoryId: LLMMemoryId, messages: List<Message>) = memoryStore[memoryId]
        ?.let {
            it.addAll(messages)
            Success(memoryId)
        }
        ?: Failure(NotFound)

    override fun update(memoryId: LLMMemoryId, messages: List<Message>): LLMResult<LLMMemoryId> {
        memoryStore[memoryId] = messages.toMutableList()
        return Success(memoryId)
    }

    override fun delete(memoryId: LLMMemoryId) = memoryStore[memoryId]
        ?.let {
            memoryStore.remove(memoryId)
            Success(memoryId)
        }
        ?: Failure(NotFound)
}


