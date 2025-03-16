package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SystemMessage private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SystemMessage>(::SystemMessage)
}
