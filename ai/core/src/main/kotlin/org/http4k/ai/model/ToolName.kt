package org.http4k.ai.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class ToolName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ToolName>(::ToolName, "^[a-zA-Z0-9_-]{1,64}$".regex)
}
