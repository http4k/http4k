package org.http4k.fanbelt.bus

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * The channel a Transport uses, so several apps can share one database or Redis.
 */
class ChannelName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ChannelName>(::ChannelName, "[a-z_][a-z0-9_]{0,62}".regex) {
        val default = of("fanbelt")
    }
}
