package org.http4k.fanbelt.input

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class FieldName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<FieldName>(::FieldName)
}
