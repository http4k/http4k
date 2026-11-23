package org.http4k.fanbelt.testing

import dev.forkhandles.time.DeterministicScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC

/**
 * A clock driven by a DeterministicScheduler.
 */
class SchedulerClock(private val scheduler: DeterministicScheduler) : Clock() {
    override fun getZone(): ZoneId = UTC

    override fun withZone(zone: ZoneId) = this

    override fun instant(): Instant = scheduler.currentTime()
}

val start: Instant = Instant.ofEpochMilli(0)
