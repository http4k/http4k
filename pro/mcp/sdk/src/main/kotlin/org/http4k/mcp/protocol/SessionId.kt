package org.http4k.mcp.protocol

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID

class SessionId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<SessionId>(::SessionId)
}
