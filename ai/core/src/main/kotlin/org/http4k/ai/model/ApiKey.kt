package org.http4k.ai.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ApiKey private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ApiKey>(::ApiKey)
}
