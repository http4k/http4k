package org.http4k.ai.mcp.protocol

import dev.forkhandles.values.ComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ProtocolVersion private constructor(value: String) : StringValue(value), ComparableValue<ProtocolVersion, String> {
    companion object : NonBlankStringValueFactory<ProtocolVersion>(::ProtocolVersion) {
        val `2024-11-05` = of("2024-11-05")
        val `2025-03-26` = of("2025-03-26")
        val `2025-06-18` = of("2025-06-18")
        val `2025-11-25` = of("2025-11-25")

        val PUBLISHED = setOf(`2024-11-05`, `2025-03-26`, `2025-06-18`, `2025-11-25`)

        val DRAFT = of("DRAFT")

        val LATEST_VERSION = PUBLISHED.max()
    }
}
