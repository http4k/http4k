package org.http4k.ai.llm.memory

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID

class LLMMemoryId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<LLMMemoryId>(::LLMMemoryId)
}
