package org.http4k.connect.github.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Owner private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Owner>(::Owner)
}
