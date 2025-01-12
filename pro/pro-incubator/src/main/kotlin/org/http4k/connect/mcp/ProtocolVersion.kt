package org.http4k.connect.mcp

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ProtocolVersion private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ProtocolVersion>(::ProtocolVersion) {
        val `2024-10-07` = ProtocolVersion.of("2024-10-07")
        val `2024-11-05` = ProtocolVersion.of("2024-11-05")

        val LATEST_VERSION = `2024-11-05`
    }
}
