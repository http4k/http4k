package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

typealias ChaosTrigger = (HttpTransaction) -> Boolean

object ChaosTriggers {
    fun Deadline(endTime: Instant, clock: Clock = Clock.systemUTC()): ChaosTrigger = { clock.instant().isAfter(endTime) }

    fun Delay(period: Duration, clock: Clock = Clock.systemUTC()): ChaosTrigger =
            clock.instant().plus(period).let { endTime -> { clock.instant().isAfter(endTime) } }

}

class SwitchTrigger(initialPosition: Boolean = false) : ChaosTrigger {
    private val on = AtomicBoolean(initialPosition)

    fun toggle(newValue: Boolean? = null) = on.set(newValue ?: !on.get())

    override fun invoke(p1: HttpTransaction): Boolean = on.get()
}
