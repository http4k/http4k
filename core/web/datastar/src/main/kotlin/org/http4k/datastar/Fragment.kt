package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents a Fragment which contains no surrounding whitespace.
 */
class Fragment private constructor(value: String) : StringValue(value.trim()) {
    companion object : NonBlankStringValueFactory<Fragment>(::Fragment)
}
