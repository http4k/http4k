package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Identifies something that can change, such as a stored state for a key. Publishing it means "re-read this".
 */
open class Topic internal constructor(value: String) : StringValue(value) {
    internal open fun placedIn(tenant: String): Topic = of("$tenant/$value")

    companion object : NonBlankStringValueFactory<Topic>(::Topic)
}
