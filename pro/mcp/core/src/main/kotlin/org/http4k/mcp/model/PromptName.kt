package org.http4k.mcp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class PromptName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PromptName>(::PromptName)
}
