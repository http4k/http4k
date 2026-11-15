package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Selects the target element of the merge process using a CSS selector.
 */
class Selector private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Selector>(::Selector)
}
