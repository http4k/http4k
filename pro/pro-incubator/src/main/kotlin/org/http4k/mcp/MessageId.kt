package org.http4k.mcp

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID

class MessageId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<MessageId>(::MessageId)
}
