package org.http4k.htmx

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Id private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Id>(::Id)
}
