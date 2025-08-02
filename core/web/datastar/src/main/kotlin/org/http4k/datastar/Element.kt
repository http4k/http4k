package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents a Fragment which contains no surrounding whitespace or inline newlines
 */
class Element private constructor(value: String) : StringValue(value.trim().replace("\n", "")) {
    companion object : NonBlankStringValueFactory<Element>(::Element)
}
