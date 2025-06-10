package org.http4k.ai.llm.image

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Size private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Size>(::Size)
}
