package org.http4k.chaos

import org.http4k.core.Request
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

typealias Trigger<T> = (T) -> Boolean

object Triggers {
    fun <T> of(trigger: (T) -> Boolean) = object : Trigger<T> {
        override fun invoke(p1: T) = trigger(p1)
    }

    fun of(trigger: () -> Boolean) = object : Trigger<Request> {
        override fun invoke(p1: Request) = trigger()
    }

    fun TimePast(period: Duration, clock: Clock = Clock.systemUTC()) = object : Trigger<Request> {
        private val endTime by lazy { AtomicReference<Instant>(clock.instant().plus(period)) }
        override fun invoke(p1: Request) = clock.instant().isAfter(endTime.get())
    }
}