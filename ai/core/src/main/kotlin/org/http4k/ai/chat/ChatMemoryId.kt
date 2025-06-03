package org.http4k.ai.chat

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID

class ChatMemoryId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<ChatMemoryId>(::ChatMemoryId)
}
