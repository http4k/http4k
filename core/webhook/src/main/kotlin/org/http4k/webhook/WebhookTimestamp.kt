package org.http4k.webhook

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.minValue
import java.time.Instant

class WebhookTimestamp private constructor(value: Long) : LongValue(value) {
    fun asInstant() = Instant.ofEpochSecond(value)

    companion object : LongValueFactory<WebhookTimestamp>(::WebhookTimestamp, 0L.minValue) {
        fun of(instant: Instant) = of(instant.epochSecond)
    }
}
