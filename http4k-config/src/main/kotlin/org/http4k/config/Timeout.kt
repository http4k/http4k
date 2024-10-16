package org.http4k.config

import java.time.Duration

data class Timeout(val value: Duration) {
    init {
        require(!value.isNegative) { "Timeout cannot be negative" }
    }
}
