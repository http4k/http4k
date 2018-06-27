package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.time.Clock
import java.time.Duration
import java.time.Instant

typealias ChaosStageTrigger = (HttpTransaction) -> Boolean

object ChaosStageTriggers {
    fun Deadline(endTime: Instant, clock: Clock = Clock.systemUTC()): ChaosStageTrigger = { clock.instant().isAfter(endTime) }

    fun Delay(period: Duration, clock: Clock = Clock.systemUTC()): ChaosStageTrigger =
            clock.instant().plus(period).let { endTime -> { clock.instant().isAfter(endTime) } }
}
