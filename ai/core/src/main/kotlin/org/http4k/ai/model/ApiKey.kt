package org.http4k.ai.model

import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ApiKey private constructor(value: String) : StringValue(value, hidden()) {
    companion object : NonBlankStringValueFactory<ApiKey>(::ApiKey)
}
