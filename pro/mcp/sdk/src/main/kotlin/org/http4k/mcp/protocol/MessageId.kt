package org.http4k.mcp.protocol

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.minValue

class MessageId private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<MessageId>(::MessageId, 1L.minValue)
}
