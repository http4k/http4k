package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Something that can change. Publishing it means "re-read this".
 */
open class Topic internal constructor(value: String) : StringValue(value) {
    internal open fun placedIn(tenant: String): Topic = of("$tenant/$value")

    companion object : NonBlankStringValueFactory<Topic>(::Topic)
}
