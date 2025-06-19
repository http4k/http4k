package org.http4k.ai.mcp.protocol

import dev.forkhandles.values.ComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ProtocolVersion private constructor(value: String) : StringValue(value), ComparableValue<ProtocolVersion, String> {
    companion object : NonBlankStringValueFactory<ProtocolVersion>(::ProtocolVersion) {
        val `2024-11-05` = ProtocolVersion.of("2024-11-05")
        val `2025-03-26` = ProtocolVersion.of("2025-03-26")
        val DRAFT = ProtocolVersion.of("DRAFT")

        val LATEST_VERSION = `2025-03-26`
    }
}
