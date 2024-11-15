package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Password private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Password>(::Password)
}
