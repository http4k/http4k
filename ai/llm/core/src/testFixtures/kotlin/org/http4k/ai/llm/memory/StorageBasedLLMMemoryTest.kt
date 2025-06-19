package org.http4k.ai.llm.memory

import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage

class StorageBasedLLMMemoryTest : LLMMemoryContract {
    override val memory = LLMMemory.Storage(Storage.InMemory())
}
