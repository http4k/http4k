package org.http4k.datastar

import dev.forkhandles.values.DurationValue
import dev.forkhandles.values.DurationValueFactory
import java.time.Duration

/**
 * Settles the element after 1000ms, useful for transitions
 */
class SettleDuration private constructor(value: Duration) : DurationValue(value) {
    companion object : DurationValueFactory<SettleDuration>({ SettleDuration(it) }) {
        val DEFAULT = SettleDuration.of(Duration.ofMillis(300))
    }
}
