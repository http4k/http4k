package org.http4k.fanbelt.bus

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * The tenant part of a topic, which a Transport can listen for separately.
 */
class Interest private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Interest>(::Interest) {
        fun of(topic: Topic) = of(topic.value.substringBefore('/'))
    }
}
