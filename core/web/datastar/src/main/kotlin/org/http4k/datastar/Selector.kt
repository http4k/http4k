package org.http4k.datastar

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * Selects the target element of the merge process using a CSS selector.
 */
class Selector private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<Selector>(::Selector, "#.+".regex)
}
