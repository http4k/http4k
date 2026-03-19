package org.http4k.connect.mpp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Currency private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Currency>(::Currency)
}
