package org.http4k.ai.llm.memory

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import dev.forkhandles.values.random
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.LLMError.NotFound
import org.http4k.ai.llm.model.Message
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.get
import org.http4k.connect.storage.remove
import org.http4k.connect.storage.set

fun LLMMemory.Companion.Storage(
    storage: Storage<List<Message>>,
    id: () -> LLMMemoryId = { LLMMemoryId.random() }
) = object : LLMMemory {
    override fun create(messages: List<Message>) = resultFrom {
        val newId = id()
        storage[newId] = messages
        newId
    }
        .mapFailure(LLMError::Internal)

    override fun read(memoryId: LLMMemoryId) = resultFrom { storage[memoryId] }
        .mapFailure(LLMError::Internal)
        .flatMap { if (it == null) Failure(NotFound) else Success(it) }

    override fun add(memoryId: LLMMemoryId, messages: List<Message>) =
        resultFrom { storage[memoryId] }
            .mapFailure(LLMError::Internal)
            .flatMap { if (it == null) Failure(NotFound) else Success(it) }
            .flatMap {
                resultFrom {
                    storage[memoryId] = it + messages
                    memoryId
                }.mapFailure(LLMError::Internal)
            }


    override fun update(memoryId: LLMMemoryId, messages: List<Message>) =
        resultFrom { storage[memoryId] }
            .mapFailure(LLMError::Internal)
            .flatMap { if (it == null) Failure(NotFound) else Success(it) }
            .flatMap {
                resultFrom {
                    storage[memoryId] = messages
                    memoryId
                }.mapFailure(LLMError::Internal)
            }

    override fun delete(memoryId: LLMMemoryId) = resultFrom {
        storage.remove(memoryId)
        memoryId
    }
        .mapFailure(LLMError::Internal)

}
