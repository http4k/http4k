package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents a HTML Fragment for Datastar SSE transport
 */
class Element private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Element>(::Element)
}
