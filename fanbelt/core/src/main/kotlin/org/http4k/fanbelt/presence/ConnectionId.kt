package org.http4k.fanbelt.presence

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID

class ConnectionId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<ConnectionId>(::ConnectionId)
}
