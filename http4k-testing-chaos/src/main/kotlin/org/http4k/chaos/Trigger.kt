package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.time.Clock
import java.time.Duration
import java.time.Instant

typealias TransactionTrigger = (HttpTransaction) -> Boolean

object Triggers {
    fun Time(endTime: Instant, clock: Clock = Clock.systemUTC()): TransactionTrigger = { clock.instant().isAfter(endTime) }

    fun TimePast(period: Duration, clock: Clock = Clock.systemUTC()): TransactionTrigger =
            clock.instant().plus(period).let { endTime -> { clock.instant().isAfter(endTime) } }
}