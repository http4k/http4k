package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents a path to a value in a data store.
 */
class Path private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Path>(::Path)
}
