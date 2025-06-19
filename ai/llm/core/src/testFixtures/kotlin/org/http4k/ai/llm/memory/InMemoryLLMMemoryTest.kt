package org.http4k.ai.llm.memory

class InMemoryLLMMemoryTest : LLMMemoryContract {
    override val memory = LLMMemory.InMemory()
}
