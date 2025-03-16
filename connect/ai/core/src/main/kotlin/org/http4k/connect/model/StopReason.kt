package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class StopReason private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<StopReason>(::StopReason)
}
