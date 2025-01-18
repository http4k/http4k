package org.http4k.mcp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ModelName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ModelName>(::ModelName)
}
