package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Script private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Script>(::Script)
}
