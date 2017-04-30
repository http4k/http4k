package org.reekwest.http.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

object TickingClock : Clock() {
    private var ticks = 0L

    override fun withZone(zone: ZoneId?): Clock = this

    override fun getZone(): ZoneId = zone

    override fun instant(): Instant = Instant.ofEpochMilli(0).plusSeconds(ticks++)
}