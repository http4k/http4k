package org.http4k.connect.kafka.rest.model

import dev.forkhandles.values.DurationValue
import dev.forkhandles.values.DurationValueFactory
import java.time.Duration

class ConsumerRequestTimeout private constructor(override val value: Duration) : DurationValue(value) {
    companion object : DurationValueFactory<ConsumerRequestTimeout>(::ConsumerRequestTimeout)
}
