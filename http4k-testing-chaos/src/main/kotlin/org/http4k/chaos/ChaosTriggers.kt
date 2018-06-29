package org.http4k.chaos

import org.http4k.core.HttpTransaction
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

typealias ChaosTrigger = (HttpTransaction) -> Boolean

object ChaosTriggers {
    /**
     * Activates after a particular instant in time.
     */
    fun Deadline(endTime: Instant, clock: Clock = Clock.systemUTC()): ChaosTrigger = { clock.instant().isAfter(endTime) }

    /**
     * Activates after a particular delay (compared to instantiation).
     */
    fun Delay(period: Duration, clock: Clock = Clock.systemUTC()): ChaosTrigger =
            clock.instant().plus(period).let { endTime -> { clock.instant().isAfter(endTime) } }

}

/**
 * Simple toggleable trigger to turn ChaosBehaviour on/off
 */
class SwitchTrigger(initialPosition: Boolean = false) : ChaosTrigger {
    private val on = AtomicBoolean(initialPosition)

    fun isActive() = on.get()

    fun toggle(newValue: Boolean? = null) = on.set(newValue ?: !on.get())

    override fun invoke(p1: HttpTransaction) = on.get()

    override fun toString() = "SwitchTrigger (active = ${on.get()})"

}

operator fun ChaosTrigger.not() = object : Function1<HttpTransaction, Boolean> {
    override fun invoke(p1: HttpTransaction): Boolean = !this@not(p1)
    override fun toString() = "NOT " + this@not.toString()
}