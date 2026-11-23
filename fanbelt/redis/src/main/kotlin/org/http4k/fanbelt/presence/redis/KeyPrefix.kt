package org.http4k.fanbelt.presence.redis

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * The prefix of presence's Redis keys, so several apps can share one Redis. Defaults to "fanbelt:presence".
 */
class KeyPrefix private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<KeyPrefix>(::KeyPrefix) {
        val default = of("fanbelt:presence")
    }
}
