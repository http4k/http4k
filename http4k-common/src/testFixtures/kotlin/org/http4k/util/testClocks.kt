package org.http4k.util

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneId.systemDefault

class TickingClock : Clock() {
    private var ticks = 0L

    fun tick(duration: Duration) {
        ticks += duration.seconds
    }

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = systemDefault()

    override fun instant(): Instant = Instant.ofEpochMilli(0).plusSeconds(ticks++)
}

object FixedClock : Clock() {
    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = systemDefault()

    override fun instant(): Instant = Instant.ofEpochMilli(0)
}
