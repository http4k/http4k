package org.http4k.ai.mcp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ToolUseId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ToolUseId>(::ToolUseId)
}
