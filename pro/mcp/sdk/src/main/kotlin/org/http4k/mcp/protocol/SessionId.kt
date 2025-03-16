package org.http4k.mcp.protocol

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SessionId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SessionId>(::SessionId)
}
