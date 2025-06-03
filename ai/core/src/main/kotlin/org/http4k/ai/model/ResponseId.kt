package org.http4k.ai.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ResponseId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ResponseId>(::ResponseId)
}
