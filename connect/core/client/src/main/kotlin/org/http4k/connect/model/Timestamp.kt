package org.http4k.connect.model

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.minValue
import java.time.Instant

/**
 * Note: This timestamp is measured to the epoch second
 */
class Timestamp private constructor(value: Long) : LongValue(value) {
    fun toInstant(): Instant = Instant.ofEpochSecond(value)

    companion object : LongValueFactory<Timestamp>(::Timestamp, 0L.minValue) {
        fun of(value: Instant) = of(value.epochSecond)
    }
}
