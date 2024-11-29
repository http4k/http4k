package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents a signal to be sent to the client - this is a JSON representation.
 */
class Signal private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Signal>(::Signal)
}
