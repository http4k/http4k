package org.http4k.connect.openfga.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class FgaObject private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<FgaObject>(::FgaObject)
}
