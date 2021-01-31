package org.http4k.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneId.systemDefault

object TickingClock : Clock() {
    private var ticks = 0L

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = systemDefault()

    override fun instant(): Instant = Instant.ofEpochMilli(0).plusSeconds(ticks++)
}

object FixedClock : Clock() {
    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = systemDefault()

    override fun instant(): Instant = Instant.ofEpochMilli(0)
}
